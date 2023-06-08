import matplotlib.pyplot as plt

# Define the data
years = [2011, 2012, 2013, 2014, 2015, 2016,
         2017, 2018, 2019, 2020, 2021, 2022, 2023]
packages = [
    278326,
    136552,
    178376,
    237086,
    345994,
    514152,
    728262,
    920570,
    1218375,
    1404258,
    1773855,
    1791565,
    690377,
]
# seed = 2023, sample% = 1%
selected = [2805, 1360, 1786, 2439, 3504, 5248,
            7353, 9229, 12138, 14067, 17927, 17869, 6707]


# Create the bar chart
plt.bar(years, packages)

# Set the labels and title
plt.xlabel("Year", fontsize=14)
plt.ylabel("Number of Maven Packages", fontsize=14)
# plt.title("Dsitribution of Sampled Maven Packages per Year", fontsize=16)
plt.title("Distribution of Published Maven Packages per Year", fontsize=16)

# Set x-axis tick locations and labels
plt.xticks(years, rotation=45, fontsize=12)
plt.yticks(fontsize=12)

# Format y-axis tick labels
plt.ticklabel_format(style="plain")

# Show the bar chart
plt.show()
