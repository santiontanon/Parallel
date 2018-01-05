import os,sys
import json
import collections
DATA_PATH = 'saved_data'
counter_ForkingMixIn = 3100

'''This is used for the web status panel'''
def get_file_status(writer):
    columns = 'id,num,user,version,start,end,len,levels,me,uploaded,filename,seq'.split(',')
    path = os.path.dirname(os.path.abspath(__file__)) + '/' + DATA_PATH
    data_dict = {}
    files = {}
    missing_files = []
    largest_id = 1
    for i in os.listdir(path + '/data'):
        with open(path + '/data/' + i) as f:
            for line in f:
                if line.startswith('filename'):
                    filename = line.split('\t')[1].strip()
                    if filename in files:
                        files[filename]+=1
                    else:
                        files[filename] = 0
    for i in os.listdir(path + '/id'):
        data = json.load(open(path+'/id/'+i))
        if data['id'] in data_dict:
            #data_dict[data['id']]['num']+=1
            data['num'] = data_dict[data['id']]['num']+1
        else:
            data['num'] = 1
        data_dict[data['id']] = data
        if data['id']==int(data['id']) and data['id'] > largest_id:
            largest_id = data['id']
    for i in os.listdir(path + '/log'):
        data = get_file_data(path + '/log/' + i,files,missing_files)
        data['filename']=i
        data['seq'] = ' '.join(data['seq'])
        if 'id' in data and data['id'].isdigit():
            data['id'] = int(data['id'])
            if data['id'] > largest_id: largest_id = data['id']
        # clean the ForkingMixIn mess:
        if True:
            if 'id' in data and data['id'] in [3087,3088]:
                global counter_ForkingMixIn
                counter_ForkingMixIn += 1
                data['id'] = counter_ForkingMixIn
                data_dict[data['id']] = data
        #if 'id' in data and data['id'] in data_dict and data['user'] == data_dict[data['id']]['user']:
        if 'id' in data:
            if data['id'] in data_dict:
                data_dict[data['id']].update(data)
            else:
                data_dict[data['id']] = data
        else:
            print "no match",data

    if writer: # used for the httpserver gui
        def html_table_from_list_of_dicts(table, labels=None, options={}):
            separator = '\n'
            table_data = html_wrap('tr', separator.join([html_wrap('th', i) for i in labels]))
            table_data += separator.join([html_wrap('tr',
                                                    separator.join([html_wrap('td', row.get(i, '')) for i in labels]))
                                          for row in table])
            return html_wrap('table border=1', table_data)

        def html_wrap(tag, contents, extra_html=None):
            return '<%s>%s</%s>' % (tag, contents, tag.split()[0])

        data = sorted(data_dict.values(),key=lambda i:i['id'])
        table = html_table_from_list_of_dicts(data,columns)
        writer.send_response(200, "OK")
        writer.send_header('Content-type', 'text/html')
        writer.send_header("Access-Control-Allow-Origin", "*")
        writer.end_headers()
        writer.wfile.write(html_wrap('html',html_wrap('body',table)))
    else:
        import pprint
        pprint.pprint(data_dict)
        print "FILES"
        for i in files:
            print i
        print "MISSING FILES"
        for i in missing_files:
            print i
    return largest_id

def get_file_data_finish(attempt,start_time):
    t_, e_, d_, s_, _ = attempt['last_line'].split('\t', 4)
    s_ = datetime.datetime.strptime(t_, '%m-%d-%y-%H-%M-%S')
    s_ = (s_ - epoch).total_seconds()
    s_ = s_ - start_time
    del(attempt['last_line'])
    attempt['end_date'] = t_
    attempt['total_time'] = (float(s_) - attempt['start_time']) #/ 60.0

def get_file_data_labels():
    return [
        'level',
        'start_date',
        'start_time',
        'end_date',
        'tutorial_time',
        'total_time',
        'me_tests',
        'me_submissions',
        'me_replays',
        'tooltips',
        'connection_visibility',
        'flow_visibility',
        'flow_tooltip',
        'popup_success',
        'popup_error',
        'popup_error_starvation',
        'popup_error_deadend',
        'popup_error_delivery',
        'popup_error_badgoals',
        'tooltip_Place_Semaphore',
        'tooltip_PlaceButton',
        'tooltip_Trash',
        'dragged',
        'trashed',
        'hover',
        'dragged_button',
        'trashed_button',
        'dragged_semaphore',
        'trashed_semaphore',
        'dragged_signal',
        'trashed_signal',
        'hover_button',
        'hover_semaphore',
        'hover_signal',
    ]

import datetime
epoch = datetime.datetime.utcfromtimestamp(0)

def new_attempt(t_,s_,level_num):
    attempt = dict([(i, 0) for i in get_file_data_labels()])
    attempt['start_date'] = t_
    attempt['start_time'] = float(s_)
    attempt['level'] = level_num
    attempt['tutorial_time'] = 0.0
    return attempt


def get_file_data(fname,files,missing_files, slice=None):
    data = {'levels': 0, 'me': 0, 'uploaded': 0,'seq':[], 'attempts':[]}
    attempt = dict([(i, 0) for i in get_file_data_labels()])
    attempt['start_time'] = 0.0
    last_line = None
    last_event_time = None
    slice_found = False
    start_time = None

    with open(fname) as f:
        for line in f:
            #print line
            try:
                t_, e_, d_, s__, _ = line.split('\t', 4)
                s_ = datetime.datetime.strptime(t_,'%m-%d-%y-%H-%M-%S')
                s_ = (s_ - epoch).total_seconds()
                if not start_time:
                    start_time = s_
                    s_ = 0.0
                else:
                    s_ = s_ - start_time

            except:
                #print "invalid line",fname,line
                continue
            if 'start' not in data:
                data['start'] = t_
            if not slice and last_event_time and s_ - last_event_time > 600:
                level_num = str(attempt['level']) + '?'
                level_num = 'Inactive'
                data['attempts'].append(attempt)
                attempt = new_attempt(t_,s_,level_num)

            if 'SessionID' not in data and e_=='SessionID':
                sid = d_
                data['id'] = sid
                try:
                    sid = int(sid)
                    if CURRENTLY_INVESTIGATING and sid not in CURRENTLY_INVESTIGATING:
                        return {}
                except:
                    pass
            if 'SessionUser' not in data and e_=='SessionUser':
                data['user'] = d_
            if slice:
                slice_user,slice_offset,slice_start,slice_end,slide_label, slice_level = slice
                if s_<(slice_start-slice_offset) and not slice_found:
                    continue
                elif s_>=(slice_start-slice_offset) and not slice_found:
                    attempt = new_attempt(t_, s_, "slice")
                    slice_found = True
                    data['slice']= attempt
                elif slice_found and s_<(slice_end-slice_offset):
                    pass # just record data
                elif slice_found and s_>=(slice_end-slice_offset):
                    # cleanup ending
                    if 'last_line' in attempt:
                        get_file_data_finish(attempt, start_time)
                    continue
                else:
                    continue
            if e_ == 'TriggerLoadLevel':
                if d_:
                    if d_.startswith('l'):
                        level_num = d_.replace('level', '').replace('-', '')
                        if level_num: data['seq'].append(level_num)
                        data['levels'] += 1
                        if not slice:
                            data['attempts'].append(attempt)
                            attempt = new_attempt(t_, s_, level_num)
                        else:
                            pass
                    else:
                        data['me'] += 1
                        if d_ in files:
                            data['uploaded'] += 1
                        else:
                            missing_files.append(d_)
                else:
                    data['seq'].append('R')
                    attempt['me_replays'] += 1
            elif e_=='endTutorial':
                attempt['tutorial_time'] = (float(s_) - attempt['start_time']) / 60.0
            elif e_=='SubmitCurrentLevelPlay':
                data['seq'].append('T')
                attempt['me_tests']+=1
            elif e_=='SubmitCurrentLevelME':
                data['seq'].append('S')
                attempt['me_submissions'] += 1
            elif e_=='tooltip':
                attempt['tooltips'] += 1
                attempt['tooltip_'+d_] = attempt.get('tooltip_'+d_,0)+1
            elif e_=='startDrag':
                attempt['dragged'] += 1
                attempt['dragged_'+d_] = attempt.get('dragged_'+d_,0)+1
            elif e_=='Destroying':
                attempt['trashed'] += 1
                attempt['trashed_'+d_] = attempt.get('trashed_'+d_,0)+1
            elif e_=='OnHoverBehavior':
                attempt['hover'] += 1
                attempt['hover_'+d_] = attempt.get('hover_'+d_,0)+1

            elif e_=='ToggleConnectionVisibility' and d_=='True':
                attempt['connection_visibility'] += 1
            elif e_ == 'ToggleFlowVisibility' and d_ == 'True':
                attempt['flow_visibility'] += 1
            elif e_ == 'LockFlowVisibility' and d_ == 'True':
                attempt['flow_tooltip'] += 1
            elif e_=='TriggerGoalPopUp':
                if 'Successfully' in d_:
                    attempt['popup_success'] += 1
                elif 'starvation' in d_:
                    attempt['popup_error_starvation'] += 1
                    attempt['popup_error'] += 1
                elif 'dead' in d_:
                    attempt['popup_error_deadend'] += 1
                    attempt['popup_error'] += 1
                elif 'deliveries' in d_:
                    attempt['popup_error_delivery'] += 1
                    attempt['popup_error'] += 1
                else :
                    attempt['popup_error_badgoals'] += 1
                    attempt['popup_error'] += 1

            if line:
                last_line = line
                attempt['last_line'] = line
                last_event_time = s_
            if data['id'] and data['id'] in USER_EXTRA_DICT:
                data['user'] = USER_EXTRA_DICT[data['id']]
    if last_line:
        data['end'] = last_line.split('\t')[0]
        try:
            data['len'] = "%.1f" % (float(last_line.split('\t')[3]))
        except:
            data['len'] = '?'
    for i in data['attempts']:
        get_file_data_finish(i, start_time)
    return data


#CURRENTLY_INVESTIGATING = [1220,1212,1229,1223,1201,1241,1238,1235,1232,1222,1219,1210,1208,1209,1205,1204,1203]
CURRENTLY_INVESTIGATING = [1241,1238]
CURRENTLY_INVESTIGATING = []
USER_EXTRA_DICT = {'1241':'3-1','1238':'3-4'}
# NOTE THAT '1241':'3-1' IS NOT TRUE, THE VIDEO FOR 3-1 IS 56 MIN LONG, THE 1241 TRACE IS ONLY 40 MINUTES LONG
# IF IT HELPS, 3-1 HITS PLAY AT 2:17PM

path = os.path.dirname(os.path.abspath(__file__)) + '/' + DATA_PATH

# This gets stats from the log files ignoring the rest of the files submitted, check get_stats for more info
def get_attempt_data(print_stats=False):
    all_attempts = []
    print 'id\tuser\t'+'\t'.join(get_file_data_labels())
    for i in os.listdir(path + '/log'):
        data = get_file_data(path + '/log/' + i,[],[])
        if not data: continue
        all_attempts.append(data)
        data['filename']=i
        data['seq'] = ' '.join(data['seq'])
        if print_stats:
            if 'user' in data and '-' in data['user']:
                #print data
                for attempt in data['attempts']:
                    print data['id']+'\t'+data['user']+'\t'+'\t'.join([str(attempt[i]) for i in get_file_data_labels()])
    return all_attempts

def print_sliced_data(all_data,slice):
    for data_ in all_data:
        if 'user' in data_ and slice[0] == data_['user']:
            data = get_file_data(path + '/log/' + data_['filename'], [], [], slice)
            if 'slice' in data:
                print data['id'] + '\t' + data['user'] + '\t' +data_['filename'] +'\t'+ '\t'.join([str(data['slice'][i]) for i in get_file_data_labels()])+'\t'+slice[-2]+'\t'+slice[-1]
            else:
                sys.stderr.write('COULD NOT FIND %s\n' % str(slice))

def print_aggregates(data):
    #groups = collections.defaultdict(lambda:collections.defaultdict(list))
    groups = collections.defaultdict(lambda: collections.defaultdict(lambda: collections.defaultdict(lambda: 0)))
    for trace in data:
        if not 'user' in trace or not trace['user'] or not '-' in trace['user']:
            continue
        group = trace['user'][0:2]
        for attempt in trace['attempts']:
            #groups[attempt['level']][group].append((trace['user'],attempt['total_time']))
            groups[attempt['level']][group][trace['user']] += attempt['total_time']
            #groups[attempt['level']][group][trace['user']] += attempt['total_time'] - attempt['tutorial_time']
    for level in sorted(groups.keys()):
        print "level: ",level
        running_lst = []
        for group in sorted(groups[level].keys()):
            print "\tgroup: ",group
            print "\t\t",
            for pair in sorted(groups[level][group].items()):
                print "\t%s\t%.2f" % pair,
                running_lst.append(pair[1])
            print
            total = sum([i[1] for i in groups[level][group].items()])
            print "\t\t\ttotal:\t%.2f\taverage:\t%.2f\t"%(total,total/len(groups[level][group]))
        print "\ttotal:\t%.2f\taverage:\t%.2f\t" % (sum(running_lst), sum(running_lst) / len(running_lst))
        print


def parse_forms_data():
    labels_a = [2,6]
    labels_b = [11,12,13,14,15,16]
    data = collections.defaultdict(dict)
    sections = ['Before','After']
    labels_ = 'How much do you think you know about Critical Sections?	How comfortable are you with spotting Critical Sections?	How much do you think you know about Starvation?	How comfortable are you with spotting Starvation?	How much do you think you know about Race Conditions?	How comfortable are you with spotting Race Conditions?'.split('\t')
    #labels = ['How old are you?','What GPA range best matches your current overall GPA?'] + labels_ + labels_

    section = None
    for line in open(DATA_PATH + '/forms.tsv').readlines()[1:]:
        if line and len(line.split())==2 and line.split()[1] in sections:
            section = line.split()[1]
        if not section: continue
        if line and len(line.split('\t'))>16:
            line_data = line.split('\t')
            if not line_data[1] or '-' not in line_data[1]: continue
            user = line_data[1]
            #if section == 'Before':
                #for col in labels_a:
            for col,col_name in zip(labels_b,labels_):
                data[user][col_name+'-'+section] = int(line_data[col])
    labels = [i+'-'+sections[0] for i in labels_] + [i+'-'+sections[1] for i in labels_]
    return labels,data




if __name__=="__main__":
    import jsonpickle
    import argparse
    import sys
    #get_file_status(None)
    print 'PARSING DATA'
    try:
        data = jsonpickle.loads(open('index-files.json','rb').read())
    except:
        data = get_attempt_data(False)
        with open('index-files.json','wb') as f:
            f.write(jsonpickle.dumps(data))
    if len(sys.argv)>1:
        # user, offset, start, end
        #slices = [('4-12', 15.0, 0.0, 120.0), ('4-12', 15.0, 120.0, 240.0), ('4-12', 15.0, 0.0, 240.0),]
        # User	Hit Play	Start Time	Start Time	End Time	End Time	A B C
        print '\t'.join((['id', 'user'] + get_file_data_labels()))
        slices = [i.split('\t') for i in open(sys.argv[1],'r').readlines()][1:]
        COL_USER = 0
        COL_LEVEL = 1
        COL_OFFSET = 2
        COL_START = 3
        COL_END = 4
        COL_LABEL = 5
        slices_ = []
        for i in slices:
            slices_.append([i[COL_USER].strip().lower(),float(i[COL_OFFSET]),float(i[COL_START]),float(i[COL_END]),i[COL_LABEL].strip().lower(),i[COL_LEVEL].strip()])
        for slice in slices_:
            print_sliced_data(data,slice)
    else:
        print 'AGGREGATES'
        print_aggregates(data)
