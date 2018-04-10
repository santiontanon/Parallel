#!/bin/env python
import os
import sys
import ast
import math
import collections
import datetime
import util
import random

epoch = datetime.datetime.utcfromtimestamp(0)

def get_feature_labels():
    return [
        #'level', # This will always be the same...
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
        'r_me_per_minute',
        'r_me_tests_per_minute',
        'r_me_submissions_per_minute',
        'r_me_replays_per_minute',
        'r_me_tests',
        'r_me_submissions',
        'r_me_replays',

        'r_num_tracks_used_per_minute',
        'r_num_track_no_changes_per_minute',
        'r_num_track_changes_per_minute',
        'r_num_mouse_on_comp_per_minute',
        'r_dragged_components_per_minute',
        'r_hover_components_per_minute',

        #'r_reposition_dist_per_minute',
        't_d_component_dragged_me_avg',
        't_d_component_dragged_me_tests_avg',
        't_d_component_dragged_me_submissions_avg',
        't_d_component_dragged_me_min', # This is apparently helpful...why...
        't_d_component_dragged_me_tests_min',
        't_d_component_dragged_me_submissions_min',

        'num_tracks_used',    # Number of tracks used at the time period
        'num_track_no_changes',
        'num_track_changes',
        'num_mouse_on_comp',    # Number of tracks used at the time period

        'total_time_on_component',  # Total amount of time on components 
        'avg_time_on_component', # Computed as: total time on component / num_mouse_on_comp

        'total_components', # Total number of components over all used track...
        'tracks_used_components_per_track_avg', # Total number of components over all used track / tracks used
        'avg_dragged_components_dist',
        'total_dragged_components_dist'
        #'total_reposition_dist'   # Total reposition euclidean distance
    ]

def get_additional_features():
    return [
        '_timestamp_last_dragged_component',
        '_timestamp_last_dragged_component',
        '_timestamp_last_connected_component',
        '_track_color_last_placed_component',
        '_t_d_component_dragged_me_lst',
        '_tracks_used_lst',

    ]

def get_features(interval_data, files, persistent_data, path):
    # Given the telemetry logs in a time window, get features
    data = {'levels': 0, 'me': 0, 'uploaded': 0,'seq':[], 'feature_vectors':[] }
    feature_vector = dict( [(i, 0) for i in get_feature_labels()] )
  
    ir_coord = ( ) # BeginReposition
    fr_coord = ( ) # EndReposition 

    il_coord = ( ) # BeginLink
    fl_coord = ( ) # LinkTo

    id_coord = ( ) # startDrag
    fd_coord = ( ) # endDrag

    #feature_vector['level'] = "slice"
    feature_vector['tutorial_time'] = 0.0

    if len(interval_data) == 0:
        return feature_vector
 
    date_, name_, data_, time_, x_, y_ = interval_data[0].split("\t")
    s_ = datetime.datetime.strptime(date_,'%m-%d-%y-%H-%M-%S')
    s_ = (s_ - epoch).total_seconds() 

    if not persistent_data['start_time']:
        persistent_data['start_time'] = s_

    feature_vector['start_date'] = date_
    feature_vector['start_time'] = float(s_ - persistent_data['start_time'])

    feature_vector['_t_d_component_dragged_me_lst'] = []
    feature_vector['_timestamp_last_dragged_component'] = 0.0

    start_time = None
    missing_files = []

    for int_data in interval_data: 
        date_, name_, data_, time_, x_, y_ = int_data.split("\t")
        s_ = datetime.datetime.strptime(date_,'%m-%d-%y-%H-%M-%S')
        s_ = (s_ - epoch).total_seconds()
        s_ = s_ - persistent_data['start_time']

        if name_ == 'TriggerLoadLevel':
            trackUsed = -1
            if data_:
                if data_.startswith('l'):
                    #print "Level (doesn't have "/data" associated with it): " , d_
                    level_num = data_.replace('level', '').replace('-', '')
                    if level_num: data['seq'].append(level_num)
                    data['levels'] += 1
                else:
                    # Read the data file, get all the components. Reset only when we start a new level (excluding reset).
                    persistent_data["comp_color_map"] = {}
                    persistent_data["comp_link_map"] = {}
                    persistent_data["comp_loc_map"] = {}
                    persistent_data["direction_layout"]  = []
                    persistent_data["color_layout"] = []
                    persistent_data["cur_track"] = -1
                    persistent_data["cur_mouse_comp"] = ""
                    persistent_data["cur_mouse_time"] = 0.0
                    persistent_data['linking'] = False

                    data['me'] += 1
                    if data_ in files:
                        dfile = open(path + '/data' + "/" + files[data_][0],'r')
                        dfiledata = [ d.strip("\n") for d in dfile.readlines() ]
#                        print "File, number of files (should be 1): {0},{1}".format(files[d_][0],len(files[d_]))
                        # Find components section, and get components
                        startParse = False
                        height = 0
                        width = 0
                        incr = 0
                        for line in dfiledata:
                            if line.startswith("board_width"):
                                width = int(line.split("\t")[1])
                            if line.startswith("board_height"):
                                height = int(line.split("\t")[1])
                            if line.startswith('DIRECTIONS'):
                                for i in range(incr+1,incr+height+1):
                                    persistent_data['direction_layout'].append(dfiledata[i])
                            if line.startswith('COLORS'):
                                for i in range(incr+1,incr+height+1):
                                    persistent_data['color_layout'].append(dfiledata[i])
                            if line.startswith('COMPONENTS'): 
                                startParse = True 
                            else:
                                if startParse:
                                    if len(line) == 0: # End of components section
                                        startParse = False 
                                    else:
                                        tmp = line.split("\t")
                                        compData = ast.literal_eval(tmp[6])
                                        if "color" in compData.keys():
                                            # CompID -> color
                                            persistent_data['comp_color_map'][tmp[0]] = int(compData["color"])
                                        if "link" in compData.keys():                             
                                            # CompID -> LinkedID
                                            persistent_data['comp_link_map'][tmp[0]] = str(compData["link"])
                            incr += 1
                        dfile.close()
                        data['uploaded'] += 1
                    else:
                        missing_files.append(d_)
            else:
                data['seq'].append('R')
                feature_vector['me_replays'] += 1
        elif name_ == 'endTutorial':
            feature_vector['tutorial_time'] = (float(s_) - feature_vector['start_time']) / 60.0
        elif name_ == 'SubmitCurrentLevelPlay':
            data['seq'].append('T')
            feature_vector['me_tests']+=1
            if feature_vector['_timestamp_last_dragged_component']:
                feature_vector['_t_d_component_dragged_me_lst'].append(('tests',float(s_)-feature_vector['_timestamp_last_dragged_component']))
        elif name_ =='SubmitCurrentLevelME':
            data['seq'].append('S')
            feature_vector['me_submissions'] += 1
            if feature_vector['_timestamp_last_dragged_component']:
                feature_vector['_t_d_component_dragged_me_lst'].append(('submissions',float(s_)-feature_vector['_timestamp_last_dragged_component']))
        elif name_ == 'tooltip':
            feature_vector['tooltips'] += 1
            feature_vector['tooltip_'+data_] = feature_vector.get('tooltip_'+data_,0)+1
        elif name_ == 'startDrag':
            feature_vector['dragged'] += 1
            feature_vector['dragged_'+data_] = feature_vector.get('dragged_'+data_,0)+1
            feature_vector['_timestamp_last_dragged_component'] = float(s_)
            # Coordinates for Start Drag 
            id_coord = ( float(x_), float(y_) )
        elif name_ == "endDrag":
            # Coordinates for End Drag
            fd_coord = ( float(x_), float(y_) )
            # Euclidean distance 
            dist = 0.0
            if ( len(id_coord) != 0 ): 
                dist = math.sqrt( math.pow((fd_coord[0]-id_coord[0]),2.0) + math.pow((fd_coord[1]-id_coord[1]),2.0) )
            else: 
                dist = 0.0
            id_coord = ()
            fd_coord = ()
            feature_vector['total_dragged_components_dist'] += dist
        elif name_ == 'Destroying':
            feature_vector['trashed'] += 1
            feature_vector['trashed_'+data_] = feature_vector.get('trashed_'+data_,0)+1
        elif name_ == 'OnHoverBehavior':
            # This implies that we are on a track...but we hover on an object though..?
            feature_vector['hover'] += 1
            feature_vector['hover_'+data_] = feature_vector.get('hover_'+data_,0)+1
        elif name_ == "BeginReposition":
            ir_coord = ( float(x_), float(y_) )
        elif name_ == "EndReposition":
            # Check to see if we swap threads. 
            if len(persistent_data['cur_mouse_comp']) == 0:
                continue;
            tmp = persistent_data['cur_mouse_comp'].split("/")
            compID = tmp[1]
            if tmp[1] not in persistent_data['comp_color_map'].keys(): 
                # Component was thrown away. 
                 continue

            feature_vector['num_tracks_used'] += 1
            color = persistent_data['comp_color_map'][tmp[1]]
            for c in persistent_data['comp_color_map'].keys():
                if color == persistent_data['comp_color_map'][c]:
                    feature_vector['total_components'] += 1 

            fr_coord = ( float(x_), float(y_) )
            # Euclidean distance ( don't actually use this, but keeping this in here for future use )
            #dist = 0.0
            #if ( len(ir_coord) != 0 ): 
            #    dist = math.sqrt( math.pow((fr_coord[0]-ir_coord[0]),2.0) + math.pow((fr_coord[1]-ir_coord[1]),2.0) )
            #else: 
            #    dist = 0.0
            ir_coord = ( )
            fr_coord = ( )
        elif name_ == "BeginLink":
            # Before we even link, we need to be over a component
            if len(persistent_data['cur_mouse_comp']) == 0:
                continue;
            tmp = persistent_data['cur_mouse_comp'].split("/")
            compID = tmp[1]
            if tmp[1] not in persistent_data['comp_color_map'].keys(): 
                # Component was thrown away. 
                continue

            feature_vector['num_tracks_used'] += 1
            color = persistent_data['comp_color_map'][tmp[1]]
            for c in persistent_data['comp_color_map'].keys():
                if color == persistent_data['comp_color_map'][c]:
                    feature_vector['total_components'] += 1 

            il_coord = ( float(x_), float(y_) ) 
        elif name_ == "LinkTo":
            persistent_data['linking'] = True
            fl_coord = ( float(x_), float(y_) ) 
            dist = 0.0
            if ( len(il_coord) != 0 ): 
                dist = math.sqrt( math.pow((fl_coord[0]-il_coord[0]),2.0) + math.pow((fl_coord[1]-il_coord[1]),2.0) )
            else: 
                dist = 0.0
            il_coord = ( )
            fl_coord = ( )
        elif name_ == "OnMouseComponent":
            feature_vector['num_mouse_on_comp'] += 1
            persistent_data['cur_mouse_comp'] = data_

            # Get current time..
            persistent_data['cur_mouse_time'] = float(time_)

            # Keep a list of components IDs and their corresponding locations
            y_ = y_.strip("\n")
            tmp = data_.split("/")
            persistent_data['comp_loc_map'][tmp[1]] = (float(x_),float(y_)) 

            if ( len(persistent_data['comp_color_map'].keys()) != 0 and tmp[1] in persistent_data['comp_color_map'] ): 
                if persistent_data['cur_track'] == -1:
                    persistent_data['cur_track'] = persistent_data['comp_color_map'][tmp[1]]
                else:
                    # Check to see if we changed tracks here
                    if persistent_data['cur_track'] != persistent_data['comp_color_map'][tmp[1]]:
                        feature_vector['num_track_changes'] += 1
                        persistent_data['cur_track'] = persistent_data['comp_color_map'][tmp[1]]
                    else:
                        # Staying on the same track
                        feature_vector['num_track_no_changes'] += 1
            if persistent_data['linking']:
                if ( len(persistent_data['comp_color_map'].keys()) != 0 ): 
                    tmp = data_.split("/")  
                    if ( tmp[1] in persistent_data['comp_color_map'] ):
                        feature_vector['num_tracks_used'] += 1
                        color = persistent_data['comp_color_map'][tmp[1]]
                        for c in persistent_data['comp_color_map'].keys():
                            if color == persistent_data['comp_color_map'][c]:
                                feature_vector['total_components'] += 1 
                persistent_data['linking'] = False  
        elif name_ == "OutMouseComponent":
            endTime = float(time_)
            feature_vector['total_time_on_component'] += (endTime - persistent_data['cur_mouse_time'])
        elif name_ == 'ToggleConnectionVisibility' and data_ == 'True':
            feature_vector['connection_visibility'] += 1
        elif name_ == 'ToggleFlowVisibility' and data_ == 'True':
            feature_vector['flow_visibility'] += 1
        elif name_ == 'LockFlowVisibility' and data_ == 'True':
            feature_vector['flow_tooltip'] += 1
        elif name_ == 'TriggerGoalPopUp':
            if 'Successfully' in data_:
                feature_vector['popup_success'] += 1
            elif 'starvation' in data_:
                feature_vector['popup_error_starvation'] += 1
                feature_vector['popup_error'] += 1
            elif 'dead' in data_:
                feature_vector['popup_error_deadend'] += 1
                feature_vector['popup_error'] += 1
            elif 'deliveries' in data_:
                feature_vector['popup_error_delivery'] += 1
                feature_vector['popup_error'] += 1
            else:
                feature_vector['popup_error_badgoals'] += 1
                feature_vector['popup_error'] += 1

    # Once we have searched this, finalize feature_vector
    last_line = interval_data[-1]
    t_, e_, d_, s_, _ = last_line.split('\t', 4)

    s_ = datetime.datetime.strptime(t_, '%m-%d-%y-%H-%M-%S')
    s_ = (s_ - epoch).total_seconds()
    s_ = s_ - persistent_data['start_time']

    feature_vector['end_date'] = t_
    feature_vector['total_time'] = (float(s_) - feature_vector['start_time']) #/ 60.0
    
    # NOTE: This should never occur
    if feature_vector['total_time'] < 0:
        print "start_time, s_: {0},{1}".format(feature_vector['start_time'],s_)
        print interval_data

    me_total = 0.0
    for i in ['tests','submissions','replays']:
        me_total += feature_vector['me_'+i]
    if feature_vector['total_time']:
        feature_vector['r_me_per_minute'] = me_total/(feature_vector['total_time']/60.0)
    for i in ['tests', 'submissions', 'replays']:
        if feature_vector['total_time']:
            feature_vector['r_me_'+i+'_per_minute'] = 1.0*feature_vector['me_'+i] / (feature_vector['total_time'] / 60.0)

            feature_vector['r_num_tracks_used_per_minute'] = 1.0*feature_vector['num_tracks_used'] / (feature_vector['total_time'] / 60.0)
            feature_vector['r_num_track_no_changes_per_minute'] = 1.0*feature_vector['num_track_no_changes'] / (feature_vector['total_time'] / 60.0)
            feature_vector['r_num_track_changes_per_minute'] = 1.0*feature_vector['num_track_changes'] / (feature_vector['total_time'] / 60.0)
            feature_vector['r_mouse_on_comp_per_minute'] = 1.0*feature_vector['num_mouse_on_comp'] / (feature_vector['total_time'] / 60.0)
            feature_vector['r_dragged_components_per_minute'] = 1.0*feature_vector['dragged'] / ( feature_vector['total_time'] / 60.0 )
            feature_vector['r_hover_components_per_minute'] = 1.0*feature_vector['hover'] / ( feature_vector['total_time'] / 60.0 )

        if feature_vector['num_mouse_on_comp'] != 0:
            feature_vector['avg_time_on_component'] = 1.0*feature_vector['total_time_on_component'] / ( 1.0*feature_vector['num_mouse_on_comp'] )
        if feature_vector['dragged'] != 0:
            feature_vector['avg_dragged_components_dist'] = 1.0*feature_vector['total_dragged_components_dist'] / ( 1.0*feature_vector['dragged'] )
        if feature_vector['num_tracks_used'] != 0:
            feature_vector['tracks_used_components_per_track_avg'] = 1.0*feature_vector['total_components'] / ( 1.0*feature_vector["num_tracks_used"] )

        if me_total:
            feature_vector['r_me_' + i] = 1.0*feature_vector['me_' + i] / me_total
    for i in ['_tests', '_submissions','']:
        lst_ = []
        for j in feature_vector['_t_d_component_dragged_me_lst']:
            if j[0]==i or not i:
                lst_.append(j[1])
        if lst_:
            feature_vector['t_d_component_dragged_me' + i + '_avg'] = util.average(lst_)
            feature_vector['t_d_component_dragged_me' + i + '_min'] = min(lst_)

    return feature_vector

def get_data_in_intervals(data,t1,t2,ind):
    tmp = []
    for i in range(0,len(data)):
        date_, name_, data_, time_, x_, y_ = data[i].split("\t")
        time_  = float(time_)
        if ( (t1 < time_) and (t2 > time_) ):
            tmp.append(data[i])  
            ind = i
    return (tmp,ind)

def read_from_log_directory(log_path,path,level_data,interval):
    ret_data = { }
    for f in os.listdir(log_path):  
        f = open(log_path + f,'r+')
        data = [ d.strip("\n") for d in f.readlines() ]
        split_data = []    
        t1 = 0
        t2 = interval
        tmp = [-1]
        i = 0

        # Get user's name for selecting slices
        uname = ""
        for d in data:
            date_, name_, data_, time_, x_, y_ = d.split("\t")
            if name_ == 'SessionUser': 
                uname = data_
                break

        # We can start a link in a different time frame...and finish in another time frame. Furthermore, we could be on a mouse position starting in a different time frame
        persistent_data = {}
        persistent_data["comp_color_map"] = {}
        persistent_data["comp_link_map"] = {}
        persistent_data["comp_loc_map"] = {}
        persistent_data["direction_layout"]  = []
        persistent_data["color_layout"] = []
        persistent_data["cur_track"] = -1
        persistent_data["cur_mouse_comp"] = ""
        persistent_data["cur_mouse_time"] = 0.0
        persistent_data["linking"] = False
        persistent_data['start_time'] = None

        while ( i != len(data)-1 ):
            (interval_data,i) = get_data_in_intervals(data,t1,t2,i)
            if i == len(data)-1:
                break
            feature_vector = get_features(interval_data, level_data, persistent_data, path)
            split_data.append(interval_data)

            #print "Feature vector: {0}".format(feature_vector)
            #print "Time interval {0} to {1} with data: {2}".format(t1,t2,tmp)

            t1 += float(interval)/2.0 # We want to stagger the time windows
            t2 = t1 + interval

            # Find the set of slices that are within this interval, restricted to the user's name
            slices = get_slices(slices_file,uname,t1,t2)

            #print "Time interval {0} to {1}. Slices seen {2}".format(t1,t2,slices)
            # Get the most common label
            labels = []
            for s in slices:
                labels.append(s[2]) 
            freq = collections.Counter(labels)

            # If there are ties with the most common label, select a random one....
            # Now we need to get features from these intervals.
            common = []
            fq = 0
            for f in freq.most_common():
                if f[1] > fq:
                    fq = f[1]
                    common = [f[0]]
                elif f[1] == fq:
                    common.append(f[0])
            label = ""
            # Instead of random, default to highest ( C > B > A )
            if len(common) == 0:
                label = "D" # Something arbitrary happens
            elif len(common) == 1:
                label = common[0]
            else:
                if "C" in common:
                    label = "C"
                elif "B" in common:
                    label = "B"
                else:
                    label = "A"

            #print "Most common {0} {1}".format(freq.most_common(),common)
            vector = [ str(feature_vector[lab]) for lab in get_feature_labels() ]
            vector.append(label)
            print ",".join(vector)

        ret_data[f] = split_data

def get_slices(slices_file,uname,t1,t2):
    f = open(slices_file,'r+')
    data = [ d.strip("\n") for d in f.readlines() ]
    slices = []
    for d in data[1:]:
        user, level, offset, start, end, label = d.split("\t") 
        if uname != user:
            continue 
        offset = int(offset)
        start = int(start)
        end = int(end)
        label = "".join(label.split())
        if (start-offset) == (end-offset):
            pass
        if ( (t1 < start-offset) and (t2 > end-offset ) ):
            # Within the time window
            slices.append((start-offset,end-offset,label) )
        elif ( (t2 > end-offset and t1 < end-offset) or (t2 > start-offset and t1 < start-offset) ):
            # We also need to consider overlapping with another time-window
            slices.append( (start-offset,end-offset,label) )
        else:
            pass 
        #print "user {0}, level {1}, offset {2}, start {3}, end {4}, label {5}".format(user,level,offset,start,end,label)
    return slices

def get_level_data(path):
    filesDict = {} 
    for i in os.listdir(path + '/data'):
        with open(path + '/data/' + i) as f:
            for line in f:
                if line.startswith('filename'):
                    filename = line.split('\t')[1].strip()
                    if filename in filesDict: 
                        print "Duplicates detected"
                        filesDict[filename].append(i) 
                    else:
                        filesDict[filename] = [i]
    return filesDict

if __name__ == "__main__": 
    if len(sys.argv) < 3:
        print "Usage: python interval.py interval slices.tsv"
        sys.exit(0)

    interval = int(sys.argv[1])
    slices_file = sys.argv[2]
    PATH = "../35_saved_data"
#    PATH = "../3456_saved_data" # TODO: To use this data, we need a slices.tsv for 4 and 6 dash

    LOGPATH = PATH + "/log/"

    level_data = get_level_data(PATH)
    vector = get_feature_labels();
    vector.append("annotation")

    # Feature labels 
    print ",".join(vector) 

    read_from_log_directory(LOGPATH,PATH,level_data,interval)
