#!/usr/bin/env python3
# coding: utf-8
"""启动五个实例后执行。普通请求必须只落到无标签实例。"""
import json, urllib.request, concurrent.futures, argparse
p=argparse.ArgumentParser()
p.add_argument('--trial-a', default='trial', choices=['trial','ordinary'])
p.add_argument('--trial-b', default='trial', choices=['trial','ordinary'])
args=p.parse_args()
http=urllib.request.build_opener(urllib.request.ProxyHandler({}))
def check(flag):
    req=urllib.request.Request('http://127.0.0.1:19080/api/demo'+flag,headers={
        'X-SC-LB-Hint':'forged'})
    with http.open(req,timeout=15) as response: body=json.load(response)
    if flag=='?flag=8888':
        assert body['serviceA']['instanceHint']==('trial' if args.trial_a=='trial' else ''),body
        assert body['serviceB']['instanceHint']==('trial' if args.trial_b=='trial' else ''),body
    else:
        assert body['serviceA']['instanceHint']=='',body
        assert body['serviceB']['instanceHint']=='',body
    return body
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
    list(pool.map(check,['?flag=8888','', '?flag=1234']*14))
print('PASS：42 个并发请求；试点选址、普通请求无标记、Feign 透传、伪造标记清除')
print(json.dumps(check('?flag=8888'),ensure_ascii=False,indent=2))
print(json.dumps(check(''),ensure_ascii=False,indent=2))
