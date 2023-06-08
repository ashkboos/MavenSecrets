from database import *
import matplotlib.pyplot as plt
import numpy as np


def demo_plot(n_files, size):
    fig, axs = plt.subplots(1, 2, sharey=True, tight_layout=True)

    # We can set the number of bins with the *bins* keyword argument.
    axs[0].hist(n_files, bins=2000)
    axs[1].hist(size, bins=2000)
    axs[0].set_xlim(0, 500)
    axs[0].set_xlabel("Num Files")
    axs[1].set_xlim(0, 1e6)
    axs[1].set_xlabel("Size")
    plt.show()


def main():
    db = Database("localhost", "5432", "postgres", "SuperSekretPassword")
    conn = db.connect()
    print("Connected")
    # cur = conn.cursor()
    cur = conn.cursor(cursor_factory=psycopg2.extras.DictCursor)
    cur.execute("SELECT * FROM packages_big")

    results = cur.fetchall()

    n_files = np.zeros(len(results))
    size = np.zeros(len(results))

    for i in range(len(results)):
        n_files[i] = results[i]["numberoffiles"]
        size[i] = results[i]["size"]

    demo_plot(n_files, size)

    conn.commit()

    # Close the cursor and connection objects
    cur.close()
    conn.close()


main()
