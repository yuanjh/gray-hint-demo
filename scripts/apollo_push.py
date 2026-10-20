#!/usr/bin/env python3
# coding: utf-8
"""通过 Apollo 自动添加/更新配置并发布，免去手动点 Portal。

两种模式（自动选择）：
1) OpenAPI 模式：设置 APOLLO_OPENAPI_TOKEN 时走开放平台 REST，适合 CI/长期自动化。
2) Portal 会话模式：未设置 token 时用管理员账号登录 Portal 走其内部接口，
   适合本地测试容器等尚无开放平台 token 的场景。

用法：
  APOLLO_PORTAL=http://<portal>:8070 APOLLO_APP_ID=gray-demo APOLLO_ENV=DEV \
  [APOLLO_OPENAPI_TOKEN=<token> | APOLLO_ADMIN_USER=apollo APOLLO_ADMIN_PASSWORD=admin] \
  python3 scripts/apollo_push.py [--prune] [配置文件，默认根目录 apollo-gray-hint.properties]

可选参数：--prune  删除命名空间内配置文件未定义的其余键（以本工程配置为准）
可选环境变量：APOLLO_CLUSTER(default)、APOLLO_NAMESPACE(application)、APOLLO_OPERATOR(操作人)
配置文件为 properties 格式：每行 key=value，# 开头为注释。
"""
import http.cookiejar
import json, os, sys, urllib.error, urllib.parse, urllib.request, pathlib

COMMENT = 'gray-hint-demo 灰度测试配置（脚本推送）'


class OpenApiMode:
    """开放平台 REST（Authorization: token）。"""

    def __init__(self, portal, token):
        self.portal, self.token = portal, token

    def base(self, env, app, cluster, ns):
        return f'{self.portal}/openapi/v1/envs/{env}/apps/{app}/clusters/{cluster}/namespaces/{ns}'

    def _call(self, method, url, body=None):
        # Apollo 官方为裸 token；个别网关要求 Bearer，401 时自动切换重试。
        for auth in (self.token, 'Bearer ' + self.token):
            data = json.dumps(body).encode('utf-8') if body is not None else None
            req = urllib.request.Request(url, data=data, method=method, headers={
                'Authorization': auth, 'Content-Type': 'application/json;charset=UTF-8'})
            try:
                with urllib.request.urlopen(req, timeout=20) as r:
                    raw = r.read()
                    return json.loads(raw) if raw else {}
            except urllib.error.HTTPError as e:
                if e.code == 401 and auth == self.token:
                    continue
                raise

    def ensure_namespace(self, base, app, env, cluster, ns, operator):
        try:
            self._call('GET', base)
        except urllib.error.HTTPError as e:
            if e.code != 404:
                raise
            self._call('POST', f'{self.portal}/openapi/v1/apps/{app}/clusters/{cluster}/namespaces', {
                'appId': app, 'clusterName': cluster, 'namespaceName': ns,
                'namespaceFormat': 'properties', 'isPublic': False,
                'comment': COMMENT, 'dataChangeCreatedBy': operator})
            print(f'已创建命名空间 {ns}')

    def upsert(self, base, key, value, operator):
        payload = {'key': key, 'value': value, 'comment': COMMENT, 'dataChangeCreatedBy': operator}
        try:
            self._call('PUT', f'{base}/items/{urllib.parse.quote(key)}',
                       {**payload, 'dataChangeLastModifiedBy': operator})
            return '更新'
        except urllib.error.HTTPError as e:
            if e.code not in (400, 404, 405):
                raise
            self._call('POST', f'{base}/items', payload)
            return '新增'

    def list_items(self, base):
        data = self._call('GET', f'{base}/items')
        return data.get('content', data) if isinstance(data, dict) else data

    def delete_item(self, base, item):
        self._call('DELETE', f"{base}/items/{urllib.parse.quote(item['key'])}")

    def release(self, base, title, operator):
        return self._call('POST', f'{base}/releases', {
            'releaseTitle': title, 'releaseComment': 'scripts/apollo_push.py 自动发布',
            'releasedBy': operator})


class PortalMode:
    """Portal 登录会话（内部接口），适合没有开放平台 token 的本地测试环境。"""

    def __init__(self, portal, user, password):
        self.portal = portal
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        data = urllib.parse.urlencode({'username': user, 'password': password}).encode()
        with self.opener.open(urllib.request.Request(f'{portal}/signin', data=data), timeout=20):
            pass  # 302 即登录成功，会话 cookie 由处理器保存

    def base(self, env, app, cluster, ns):
        return f'{self.portal}/apps/{app}/envs/{env}/clusters/{cluster}/namespaces/{ns}'

    def _call(self, method, url, body=None):
        data = json.dumps(body).encode('utf-8') if body is not None else None
        req = urllib.request.Request(url, data=data, method=method, headers={
            'Content-Type': 'application/json;charset=UTF-8'})
        with self.opener.open(req, timeout=20) as r:
            raw = r.read()
            return json.loads(raw) if raw else {}

    def ensure_namespace(self, base, app, env, cluster, ns, operator):
        try:
            self._call('GET', f'{base}/items')
        except urllib.error.HTTPError as e:
            if e.code != 404:
                raise
            self._call('POST', f'{self.portal}/apps/{app}/namespaces', [{
                'env': env,
                'namespace': {'appId': app, 'clusterName': cluster, 'namespaceName': ns,
                              'namespaceFormat': 'properties', 'isPublic': False,
                              'comment': COMMENT, 'dataChangeCreatedBy': operator}}])
            print(f'已创建命名空间 {ns}')

    def upsert(self, base, key, value, operator):
        items = self._call('GET', f'{base}/items')
        existing = next((i for i in items if i.get('key') == key), None)
        payload = {'key': key, 'value': value, 'comment': COMMENT}
        if existing:
            self._call('PUT', f'{base}/item', {**payload, 'id': existing['id']})
            return '更新'
        self._call('POST', f'{base}/item', payload)
        return '新增'

    def list_items(self, base):
        return self._call('GET', f'{base}/items')

    def delete_item(self, base, item):
        self._call('DELETE', f"{base}/items/{item['id']}")

    def release(self, base, title, operator):
        return self._call('POST', f'{base}/releases', {
            'releaseTitle': title, 'releaseComment': 'scripts/apollo_push.py 自动发布',
            'releasedBy': operator, 'isEmergencyPublish': False})


def parse_properties(path):
    items = []
    for line in pathlib.Path(path).read_text(encoding='utf-8').splitlines():
        s = line.strip()
        if not s or s.startswith('#'):
            continue
        key, _, value = s.partition('=')
        items.append((key.strip(), value.strip()))
    return items


def main():
    try:
        portal = os.environ['APOLLO_PORTAL'].rstrip('/')
        app_id = os.environ['APOLLO_APP_ID']
    except KeyError as e:
        raise SystemExit(f'缺少环境变量 {e}，至少需要 APOLLO_PORTAL / APOLLO_APP_ID')
    env = os.environ.get('APOLLO_ENV', 'DEV')
    cluster = os.environ.get('APOLLO_CLUSTER', 'default')
    namespace = os.environ.get('APOLLO_NAMESPACE', 'application')
    operator = os.environ.get('APOLLO_OPERATOR', 'apollo')
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    prune = '--prune' in sys.argv
    props_file = args[0] if args else \
        str(pathlib.Path(__file__).resolve().parents[1] / 'apollo-gray-hint.properties')

    token = os.environ.get('APOLLO_OPENAPI_TOKEN')
    if token:
        client, mode = OpenApiMode(portal, token), 'OpenAPI'
    else:
        client, mode = PortalMode(portal,
                                  os.environ.get('APOLLO_ADMIN_USER', 'apollo'),
                                  os.environ.get('APOLLO_ADMIN_PASSWORD', 'admin')), 'Portal 会话'
    print(f'模式：{mode}，目标：{portal} / {env} / {app_id} / {cluster} / {namespace}')

    base = client.base(env, app_id, cluster, namespace)
    client.ensure_namespace(base, app_id, env, cluster, namespace, operator)

    parsed = parse_properties(props_file)
    if prune:  # 以配置文件为准，删除命名空间里其余不相干的键
        keep = {k for k, _ in parsed}
        for item in client.list_items(base):
            if item.get('key') not in keep:
                client.delete_item(base, item)
                print(f"删除 {item.get('key')}")

    for key, value in parsed:
        print(f'{client.upsert(base, key, value, operator)} {key} = {value}')

    release = client.release(base, 'gray-hint-demo 灰度测试配置', operator)
    print(f"发布成功：releaseId={release.get('id')}，环境={env}，应用={app_id}，命名空间={namespace}")


if __name__ == '__main__':
    main()
