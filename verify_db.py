from db import get_connection, fetch_query

try:
    connection = get_connection()
    print("Database connection successful!")
    connection.close()

    doctors = fetch_query("SELECT doctor_id, doctor_name FROM doctor ORDER BY doctor_id")

    print("Dictionary query execution successful!")
    print(doctors)

except Exception as error:
    print("Error:", error)