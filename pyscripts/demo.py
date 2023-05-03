from database import *
import matplotlib.pyplot as plt

def plot_n_files(n_files):
    plt.plot()

def main(self):
    db = Database('localhost','5432', 'postgres', 'SuperSekretPassword')
    conn = db.connect()
    print("Connected")
    cur = conn.cursor()
    cur.execute('SELECT * FROM packages')

    results = cur.fetchall()

    print(results)

    conn.commit()

    # Close the cursor and connection objects
    cur.close()
    conn.close()
        
main()