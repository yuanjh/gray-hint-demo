#!/usr/bin/env python3
"""无需 IDEA 的等效启动方法：前台运行单个进程，Ctrl+C 停止。无 Agent 参数。"""
import argparse,pathlib,os
p=argparse.ArgumentParser();p.add_argument('role',choices=['gateway','a-main','a-trial','b-main','b-trial']);args=p.parse_args()
roles={'gateway':('gateway',19080,'main'),'a-main':('service-a',19091,'main'),'a-trial':('service-a',19093,'trial'),'b-main':('service-b',19092,'main'),'b-trial':('service-b',19094,'trial')}
mod,port,lane=roles[args.role];root=pathlib.Path(__file__).resolve().parents[1];jar=root/mod/'target'/(mod+'.jar')
if not jar.exists():raise SystemExit('请先用 IDEA Maven package 或 mvn package 编译工程')
os.execvp('java',['java','-jar',str(jar),'--server.port='+str(port),'--demo.lane='+lane,'--spring.profiles.active='+('trial' if lane=='trial' else 'default')])
