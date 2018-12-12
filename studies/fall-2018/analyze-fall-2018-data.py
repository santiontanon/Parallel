import os
import argparse
import json
import datetime
from operator import *

# Parse data into the following format: user, start time for run, end time for run, skill vector, level id

output_directory = "pcg-levels/"

restrict_skills = True
restricted_skill_map = {
    "Understand the use of semaphores": "Seamphores Predicated",
    # TODO: Make sure this exists in the list of rules
    # "Alternating access with semaphores and buttons (ensure mutual exclusion)" : "Race Condition Predicated",
    "Alternating access with semaphores and buttons (ensure mutual exclusion)" : "Mutual Exclusion Predicated",
    "Prevent starvation" : "Starvation Predicated",
    "Block critical sections" : "Critical Section Predicted",
    "Understand that arrows move at unpredictable rates": "Randomness Predicted",
    "Synchronize multiple arrows": "Syncronization Predicted"
}

def process_data_study(study_data_abs_path):
    processed_study_data = dict()
    data_dirs = os.listdir(study_data_abs_path)
    for dir_ in data_dirs:
        absolute_path = study_data_abs_path + "/" + dir_
        # print("Retrieving data from directory: {}".format(absolute_path))
        study_files = os.listdir(absolute_path)
        study_files = [(absolute_path + "/" + d) for d in study_files]
        processed_study_data[dir_] = study_files
    return processed_study_data

def get_players(processed_study_data):
    player_list = list()
    id_files = processed_study_data["id"]
    for id_file in id_files:
        with open(id_file) as f:
            json_data = json.load(f)
            player = json_data['user']
            if player not in player_list:
                player_list.append(player)
    # print(player_list, ",", len(player_list))
    return player_list

def process_me_execution_files(processed_study_data):
    me_files = processed_study_data["data"]
    key_to_me_execution_file = dict()
    for me_file in me_files:
        with open(me_file) as f:
            key = f.readline().strip("\n")
            key = key.split("\t")[1]
            key_to_me_execution_file[key] = me_file
            continue
    return key_to_me_execution_file

def get_player_modeling_data(processed_study_data, key_to_me_execution_file):
    pm_files = processed_study_data["playermodel"]
    player_to_modeling_data = dict()
    for pm_file in pm_files:
        with open(pm_file) as f:
            json_data = json.load(f)
            user = "undefined"
            if 'user' in json_data:
                user = json_data['user']
            # print("User: {}".format(user))
            if user not in player_to_modeling_data:
                player_to_modeling_data[user] = list()
            for level in list(json_data.keys()):
                if level == "user" or level == "current":
                    continue
                pcg_pm_data = json_data[level]
                for pm_data in pcg_pm_data:
                    me_file_key = pm_data['meout']
                    skill_vector = pm_data['sv']
                    start_time = pm_data['start']
                    end_time = pm_data['end']
                    player_to_modeling_data[user].append((start_time, end_time,
                            skill_vector, key_to_me_execution_file[me_file_key], level))
    return player_to_modeling_data

def create_analysis_data(processed_study_data):
    epoch = datetime.datetime.utcfromtimestamp(0)
    file_key_to_me_execution_file = process_me_execution_files(processed_study_data)
    title_printed = False
    analysis_data = dict()
    title = ""
    player_to_modeling_data = get_player_modeling_data(processed_study_data, file_key_to_me_execution_file)
    for user, modeling_data_list in player_to_modeling_data.items():
        pcg_level_index = 1
        level_data_to_index = dict()
        for start_time, end_time, skill_vector, me_filepath, level in modeling_data_list:
            skill_data_list = [skill_data.split(",") for skill_data in skill_vector.split(":")]
            skill_confidence_list = list()
            skill_name_list = list()
            restricted_skill_list = list(restricted_skill_map.keys())
            for skill_name, skill_confidence, skill_evidence in skill_data_list:
                if skill_name not in restricted_skill_list:
                    continue
                skill_confidence_list.append(skill_confidence)
                skill_name_list.append(skill_name)

            if not title_printed:
                title_printed = True
                title = "user,start_time,end_time,level_name,{}".format(",".join(skill_name_list))

            if level == "level-1":
                filename = me_filepath.split("/")[-1]
                fp = open(me_filepath, 'r')
                data = fp.readlines()
                fp.close()
                data = [d.strip("\n") for d in data]

                direction_string = ""
                board_height = 0
                for i in range(0,len(data)):
                    if "board_height" in data[i]:
                        board_height = int(data[i].split("\t")[1])
                        continue
                    if "DIRECTIONS" in data[i]:
                        for j in range(i, i+board_height):
                            direction_string += data[j]
                        break

                if direction_string not in level_data_to_index:
                    level_data_to_index[direction_string] = pcg_level_index
                    pcg_level_index += 1

            if not level == "level-1":
                level_str_rep = level
            else:
                level_str_rep = "X{}".format(level_data_to_index[direction_string])

            if user not in analysis_data:
                analysis_data[user] = ["{},{},{},{},{}".format(user.replace("-", "."), start_time, end_time,
                level_str_rep, ",".join(skill_confidence_list))]
            else:
                analysis_data[user].append("{},{},{},{},{}".format(user.replace("-", "."), start_time, end_time,
                level_str_rep, ",".join(skill_confidence_list)))

    # TODO: sort data based on start and end time.
    analysis_data_sorted = list()
    completion_time_data = list()
    for user, data in analysis_data.items():
        time_data_pair_list = list()
        for d in data:
            start_time = d.split(",")[1]
            s_ = datetime.datetime.strptime(start_time,'%m-%d-%y-%H-%M-%S')
            s_ = (s_ - epoch).total_seconds()
            time_data_pair_list.append((s_, d))
        sorted_data = sorted(time_data_pair_list, key=itemgetter(0))
        # print("---------- user: {} ----------".format(user))
        levels_printed = list()
        sorted_data.reverse()
        for t_, d in sorted_data:
            level_str_rep = d.split(",")[3]
            if level_str_rep not in levels_printed:
                levels_printed.append(level_str_rep)
                analysis_data_sorted.append(d)

        level_to_level_data = dict()
        for t_, d in sorted_data:
            level_str_rep = d.split(",")[3]
            if level_str_rep not in level_to_level_data:
                level_to_level_data[level_str_rep] = [d]
            else:
                level_to_level_data[level_str_rep].append(d)

        for level_name, level_data in level_to_level_data.items():
            # if level_name != "level7" and level_name != "level13":
            #     continue
            start_time = level_data[-1].split(",")[1]
            end_time = level_data[0].split(",")[2]
            start_time_ = datetime.datetime.strptime(start_time,'%m-%d-%y-%H-%M-%S')
            end_time_ = datetime.datetime.strptime(end_time,'%m-%d-%y-%H-%M-%S')
            start_time_ = (start_time_ - epoch).total_seconds()
            end_time_ = (end_time_ - epoch).total_seconds()
            completion_time_data.append("{},{},{}".format(user, level_name, end_time_ - start_time_))
    # print(title)
    # for d in analysis_data_sorted:
    #     print(d)

    print("user,level,completion time (seconds)")
    for d in completion_time_data:
        print(d)

def create_pcg_analysis_files(processed_study_data):
    global output_directory
    if not(os.path.isdir(output_directory)):
        os.makedirs(output_directory)
    file_key_to_me_execution_file = process_me_execution_files(processed_study_data)
    player_to_modeling_data = get_player_modeling_data(processed_study_data, file_key_to_me_execution_file)
    for user, modeling_data_list in player_to_modeling_data.items():
        for start_time, end_time, skill_vector, me_filepath in modeling_data_list:
            filename = me_filepath.split("/")[-1]
            fp = open(me_filepath, 'r')
            data = fp.readlines()
            fp.close()

            fp = open(output_directory + filename,'w')
            fp.write("start\t{}\n".format(start_time))
            fp.write("end\t{}\n".format(end_time))
            fp.write("skill_vector\t{}\n".format(skill_vector))
            for d in data:
                fp.write(d)
            fp.close()

if __name__ == "__main__":
    args
    processed_study_data = process_data_study("saved_data-11-12-2018/")
    get_players(processed_study_data)
    # processed_study_data = process_data_study("saved_data/")
    # create_pcg_analysis_files(processed_study_data)
    create_analysis_data(processed_study_data)
