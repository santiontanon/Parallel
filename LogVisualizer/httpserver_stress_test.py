import urllib2
import threading
import time

data = open('/Users/josepvalls/test.log').read()
url = 'http://129.25.12.216:8787/log' # cci cloud "centos"
url = 'http://144.118.172.191:8787/log' # santi's "magic" server

def worker():
    print "starting"
    start = time.time()
    try:
        resp = urllib2.urlopen(url, data=data).read()
        print time.time() - start, resp
    except:
        print time.time() - start, "ERROR READING"



threads = []
for i in range(20):
    t = threading.Thread(target=worker)
    threads.append(t)
    t.start()

