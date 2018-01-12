import sys, os
import collections, operator
import util

def get_traces_old(path):
    print 'Reading from',path
    for root, dirs, files in os.walk(path):
        for i in files:
            if i.endswith('.log'):
                process(root + '/' + i)
def get_traces_new():
    path = "/Users/josepvalls/paraprog/LogVisualizer/saved_data/log/"
    for i in os.listdir(path):
        process(path+i)

def main():
    path = '/Users/josepvalls/paraprog/data/traces/'
    get_traces_old(path)
    get_traces_new()
    print 'Overview'
    strategy_all_traces(traces)
    print 'Writing labels'
    for label_name,label_i in [('user',0),('level',1)]:
        with open(path+'labels-%s.tsv' % label_name, 'w') as f:
            for label in enumerate(labels):
                l = label[1][label_i]
                if label_name == 'user':
                    if '-' not in l: l='Test'
                f.write('%d\t%s\n' % (label[0],l))
    sys.exit()
    for func_name,func in [('edit_distance',util.string_edit_distance),('markov_laplace_n4', markov_laplace_n),('markov_inter_n4',markov_inter_n)]:
        print 'Writing', func_name
        with open(path+'m-%s.tsv' % func_name, 'w') as f:
            for trace_i,trace in enumerate(traces):
                f.write(str(trace_i))
                f.write('\t')
                for trace_ in traces:
                    dist = func(trace,trace_)
                    f.write(str(dist))
                    f.write('\t')
                f.write('\n')
    print 'Done'

#################
### Loading stuff
#################

EVENT_NAME = 1
EVENT_DATA = 2

events = {
    ('startDrag', 'button'): 'W',
    # ('BeginLink','semaphore'):'L',
    ('LinkTo', None): 'L',
    ('EndReposition', None): 'D',
    ('Destroying', None): 'T',
    ('SubmitCurrentLevelPlay', None): 'P',
    ('SubmitCurrentLevelME', None): 'S',

}

events = {
    'startDrag': 'WS',
    'LinkTo': 'WL',
    'EndReposition': 'WD',
    'Destroying': 'WT',
    'SubmitCurrentLevelPlay': 'P',
    'SubmitCurrentLevelME': 'S',
    'ToggleFlowVisibility': 'IF',
    'nextTutorial': 'IT',
    'RevealGridColorMask':'IC',
    #'OnHoverBehavior':'IL'
}

events = {
    'startDrag': 'D',
    'LinkTo': 'L',
    'EndReposition': 'R',
    'Destroying': 'T',
    'SubmitCurrentLevelPlay': 'P',
    'SubmitCurrentLevelME': 'S',
    'ToggleFlowVisibility': 'I',
    'RevealGridColorMask':'I',
    'OnHoverBehavior':'I'
}


traces = []
labels = []
def process(f):
    print "Loading",f
    user=level=None
    log = [i.split('\t') for i in open(f).readlines()]
    last = None
    trace = []
    for event in log:
        if len(event) < 3: continue
        if event[EVENT_NAME]=='SessionUser':
            user = event[EVENT_DATA]
        if event[EVENT_NAME] == 'TriggerLoadLevel' and event[EVENT_DATA].startswith('level0'):
            level = event[EVENT_DATA]
            print user,level
            last = None
            trace = []
            traces.append(trace)
            labels.append((user,level))
        if event[EVENT_NAME] in events:
            ret = events[event[EVENT_NAME]]
            ret = ret[0]
            if ret!=last:
                trace.append(ret)
                last=ret

#################
### Writing stuff
#################

def jaccard_distance(a,b):
    inter,union=0.0,0.0
    for a_,b_ in zip(a,b):
        inter += min(a_,b_)
        union += max(a_,b_)
    return 1.0 - (inter/union) if union else 0.5

import math
def euclidean_distance(a,b):
    dist = 0.0
    for a_, b_ in zip(a, b):
        dist += (a_-b_)**2
    return math.sqrt(dist)

def trace_to_dict(t,n):
    t_ = ['[']+t+[']']
    d = collections.defaultdict(lambda:0)
    for i in util.sliding_window(t_,n):
        d[tuple(i)]+=1
    return dict(d)

def intersect_sparse_vectors(d1,d2):
    keys = set(d1.keys()) & set(d2.keys())
    v1 = []
    v2 = []
    for k in keys:
        v1.append(d1[k])
        v2.append(d2[k])
    return v1,v2

def laplace_union_vectors(d1,d2):
    keys = set(d1.keys()) | set(d2.keys())
    v1 = []
    v2 = []
    for k in keys:
        v1.append(d1.get(k,0)+1)
        v2.append(d2.get(k,0)+1)
    for i in range(len(keys)):
        v1[i] = 1.0 * v1[i] / sum(v1)
        v2[i] = 1.0 * v2[i] / sum(v2)
    return v1,v2

def markov_inter_n(t1,t2):
    ngram_size = 4
    v1 = trace_to_dict(t1,ngram_size)
    v2 = trace_to_dict(t2,ngram_size)
    v1,v2 = intersect_sparse_vectors(v1,v2)
    return euclidean_distance(v1,v2)

def markov_laplace_n(t1,t2):
    ngram_size = 4
    v1 = trace_to_dict(t1,ngram_size)
    v2 = trace_to_dict(t2,ngram_size)
    v1,v2 = laplace_union_vectors(v1,v2)
    return jaccard_distance(v1,v2)

#################
### Other stuff
#################

def strategy_all_traces(traces):
    for size in range(1, 20):
        ngrams = []
        for trace in traces:
            ngrams += util.sliding_window(''.join(trace),size)
        print collections.Counter(ngrams).most_common(3)


if __name__=='__main__':
    main()