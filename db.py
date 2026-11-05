import os
import psycopg2
from psycopg2.extras import RealDictCursor

def get_connection():
    return psycopg2.connect(
        host="localhost",
        database="hospital_db",
        user="postgres",
        password=os.environ["DB_PASSWORD"],
        port="5432"
    )

def fetch_query(query, params=None):
    connection = get_connection()
    try:
        with connection.cursor(cursor_factory=RealDictCursor) as cursor:
            cursor.execute(query, params)
            return cursor.fetchall()
    finally:
        connection.close()

def execute_query(query, params=None):
    connection = get_connection()
    try:
        with connection.cursor() as cursor:
            cursor.execute(query, params)
        connection.commit()
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()