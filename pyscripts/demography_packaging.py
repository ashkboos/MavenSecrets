import matplotlib.pyplot as plt
import psycopg2

from pyscripts.database import Database


def main():
    db = Database('localhost', '5432', 'postgres', 'SuperSekretPassword')
    conn = db.connect()
    cur = conn.cursor(cursor_factory=psycopg2.extras.DictCursor)

    result_difference = get_frequency_of_difference(cur)
    results_frequency = get_frequency_from_pom(cur)

    total_difference_in_packages(cur)

    print_result_with_individual_freq(result_difference)

    # Close the cursor and connection
    cur.close()
    conn.close()

    # Create a dictionary to store the "packagingType" values and their counts
    packaging_type_counts = {}
    for row in results_frequency:
        packaging_type = row[0]
        count = row[1]
        packaging_type_counts[packaging_type] = count

    s = 0
    for r in packaging_type_counts:
        s = s + packaging_type_counts[r]
        print(f'Actual Packaging type (from POM) : {r}, Count: {packaging_type_counts[r]}')

    print('Total:', s)
    # Create a bar chart
    packaging_type_counts = {key: value for key, value in packaging_type_counts.items() if value is not None}

    plt.bar(packaging_type_counts.keys(), packaging_type_counts.values())

    # Add labels and title
    plt.xlabel('Packaging Type')
    plt.ylabel('Count')
    plt.title('Number of Packages by Packaging Type')

    plt.xticks(rotation=45, ha='right')
    x_tick_labels = [packaging_type[:5] + '...' if len(packaging_type) > 5 else packaging_type for packaging_type in
                     packaging_type_counts.keys()]
    plt.xticks(list(packaging_type_counts.keys()), x_tick_labels)

    # Show the chart
    plt.show()


def get_frequency_of_difference(cur):
    cur.execute('SELECT packagingtypefrompom, packagingtypefromrepo, COUNT(*) FROM packages WHERE '
                'packagingtypefrompom != packagingtypefromrepo GROUP BY '
                'packagingtypefrompom, packagingtypefromrepo')

    results = cur.fetchall()
    return results


def get_frequency_from_pom(cur):
    cur.execute('SELECT packagingtypefrompom, COUNT(*) FROM packages WHERE packagingtypefrompom IS NOT NULL GROUP BY '
                'packagingtypefrompom')
    results = cur.fetchall()
    return results


def get_frequency_from_repo(cur):
    cur.execute('SELECT packagingtypefromrepo, COUNT(*) FROM packages GROUP BY packagingtypefromrepo')

    results = cur.fetchall()
    return results


def total_difference_in_packages(cur):
    cur.execute('SELECT COUNT(*) FROM packages WHERE packagingtypefrompom != packagingtypefromrepo')

    result = cur.fetchone()[0]
    print(f'The number of rows where packagingtypefrompom is different from packagingtypefromrepo is: {result}')


def print_result_with_individual_freq(results):
    for row in results:
        packagingtypefrompom = row[0]
        packagingtypefromrepo = row[1]
        frequency = row[2]
        print(f'Packaging type from POM: {packagingtypefrompom}, Packaging type from Repo: {packagingtypefromrepo}, '
              f'Frequency: {frequency}')


main()
