import boto3
import os
from botocore.exceptions import ClientError

AWS_ACCESS_KEY = 'test-key'
AWS_SECRET_KEY = 'test-secret'
BUCKET_NAME = 'NOT_SET'
BUCKET_NAME_PREFIX = 'raw_news/'
S3_ENDPOINT_URL = 'http://localhost:4566'
REGION_NAME = 'us-east-1'

pwd = os.getcwd()
LOCAL_DIRECTORY_PATH = os.path.join(pwd, "raw_news")

def create_s3_client():
    return boto3.client(
        's3',
        endpoint_url=S3_ENDPOINT_URL,
        aws_access_key_id=AWS_ACCESS_KEY,
        aws_secret_access_key=AWS_SECRET_KEY,
        region_name=REGION_NAME
    )


def ensure_bucket_exists(s3_client, bucket_name):
    try:
        s3_client.head_bucket(Bucket=bucket_name)
    except ClientError as e:
        error_code = e.response['Error']['Code']
        if error_code == '404':
            print(f"Bucket '{bucket_name}' not found, creating...")
            s3_client.create_bucket(Bucket=bucket_name)
            print(f"Bucket '{bucket_name}' created successfully.")
        elif error_code == '403':
            print(f"Access denied to bucket '{bucket_name}'.")
            return False
        else:
            raise
    return True

def upload_directory_to_s3(local_dir_path, bucket):
    if not os.path.isdir(local_dir_path):
        print(f"Error: Folder '{local_dir_path}' does not exist or is not a directory.")
        return

    s3_client = create_s3_client()

    if not ensure_bucket_exists(s3_client, bucket):
        return

    total_files = 0
    print(f"Starting upload from directory: {local_dir_path}")

    for root, _, files in os.walk(local_dir_path):
        for filename in files:
            local_path = os.path.join(root, filename)

            s3_key = BUCKET_NAME_PREFIX + os.path.relpath(local_path, local_dir_path)

            try:
                s3_client.upload_file(local_path, bucket, s3_key)
                print(f"   [+] Uploaded: {s3_key}")
                total_files += 1
            except ClientError as e:
                print(f"   [!] Error uploading file {s3_key}: {e}")
            except Exception as e:
                print(f"   [!] Unexpected error: {e}")

    print(f"\n Upload complete. Total files uploaded: {total_files}")
    print(f"Check your files at: {S3_ENDPOINT_URL}/{bucket}/")


if __name__ == '__main__':
    os.makedirs(LOCAL_DIRECTORY_PATH, exist_ok=True)

    upload_directory_to_s3(LOCAL_DIRECTORY_PATH, BUCKET_NAME)

