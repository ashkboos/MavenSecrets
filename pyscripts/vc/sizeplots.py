from database import *
import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns
import scipy.stats as stats


def plot_size_distribution(size):

    plt.hist(size, bins=10000)
    plt.xlim(0, 1500)
    plt.xlabel("Size (KB)")
    plt.ylabel("Frequency")
    plt.show()


def plot_correlation_graph(size, files):
    p = sns.scatterplot(x=size, y=files)
    plt.xlabel("Size (KB)")
    plt.ylabel("Number of files")
    plt.show()


def plot_correlation_graph_direct_dep(size, direct_dep):
    p = sns.scatterplot(x=size, y=direct_dep)
    plt.xlabel("Size (KB)")
    plt.ylabel("Number of direct dependencies")
    plt.show()
    t_statistic, p_value = stats.ttest_ind(size, direct_dep)
    print("T-Statistic:", t_statistic)
    print("P-Value:", p_value)



def plot_correlation_graph_trans_dep(size, trans_dep):
    p = sns.scatterplot(x=size, y=trans_dep)
    plt.xlabel("Size (KB)")
    plt.ylabel("Number of transitive dependencies")
    plt.show()
    t_statistic, p_value = stats.ttest_ind(size, trans_dep)
    print("T-Statistic:", t_statistic)
    print("P-Value:", p_value)

def main():
    db = Database('localhost', '5469', 'postgres', 'SuperSekretPassword')
    conn = db.conn
    print("Connected")
    # cur = conn.cursor()
    cur = conn.cursor(cursor_factory=psycopg2.extras.DictCursor)
    cur.execute('SELECT * FROM packages')

    results = cur.fetchall()
    count = 0
    count_direct = 0
    for i in range (len(results)):
        if not results[i]['size'] is None:
            count = count + 1
            if not results[i]['directdependencies'] is None:
                count_direct = count_direct + 1
    n_files = np.zeros(count)
    size = np.zeros(count)
    direct_dep = np.zeros(count_direct)
    size_direct = np.zeros(count_direct)

    count = 0
    for i in range (len(results)):
        if results[i]['transitivedependencies'] != -1 and not results[i]['size'] is None and not results[i]['transitivedependencies'] is None:
            count = count + 1
    trans_dep = np.zeros(count)
    size_trans = np.zeros(count)
    size_index = 0
    direct_index = 0
    trans_index = 0
    for i in range(len(results)):
        if not results[i]['size'] is None:
            n_files[size_index] = results[i]['numberoffiles']
            size[size_index] = results[i]['size'] / 1000
            if not results[i]['directdependencies'] is None:
                direct_dep[direct_index] = results[i]['directdependencies']
                size_direct[direct_index] = results[i]['size'] / 1000
                direct_index = direct_index + 1
            if results[i]['transitivedependencies'] != -1 and not results[i]['transitivedependencies'] is None:
                trans_dep[trans_index] = results[i]['transitivedependencies']
                size_trans[trans_index] = results[i]['size'] / 1000
                trans_index = trans_index + 1
            size_index = size_index + 1


    print('Mean: ' + str(np.mean(size)))
    print('Median: ' + str(np.median(size)))
    print('Std: ' + str(np.std(size)))
    print('Max: ' + str(np.max(size)))
    print('Min: ' + str(np.min(size)))
    plot_size_distribution(size)
    plot_correlation_graph(size, n_files)
    plot_correlation_graph_direct_dep(size_direct, direct_dep)
    plot_correlation_graph_trans_dep(size_trans, trans_dep)
    conn.commit()

    # Close the cursor and connection objects
    cur.close()
    conn.close()


main()
