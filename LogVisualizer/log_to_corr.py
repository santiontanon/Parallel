from string import ascii_letters
import log_analysis

labels = log_analysis.get_file_data_labels()[4:]
#labels = []
all_data = log_analysis.get_attempt_data()
data = []

form_labels,form_data = log_analysis.parse_forms_data()

for user_data in all_data:
    if not 'user' in user_data or not '-' in user_data['user']: continue
    user = user_data['user']
    for attempt in user_data['attempts']:
        data.append([attempt[i] for i in labels]+[form_data.get(user,{}).get(i,-1) for i in form_labels])


import numpy as np
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt


print labels
d = pd.DataFrame(data=data, columns=labels+form_labels)

d = d.apply(lambda x: (x - np.mean(x)) / (np.max(x) - np.min(x)))

print d

# Compute the correlation matrix
corr = d.corr()

sns.set()
sns.heatmap(corr, linewidths=.5)


plt.show()