""" This is the Lambda handler for the Wombat's python environment. """
import sys, traceback
import os

# Import the Wombats standard library
from wombats import *

envs = ['AWS_ACCESS_KEY', 'AWS_ACCESS_KEY_ID', 'AWS_SECRET_ACCESS_KEY', \
        'AWS_SECRET_KEY', 'AWS_SECURITY_TOKEN', 'AWS_SESSION_TOKEN']

def handle_event(event, time_left):
    """ Handles the execution of a Wombat's code"""

    # Remove sensitive environment variables
    for key in envs:
        if key in os.environ:
            del os.environ[key]

    try:
        # The python function must be called "wombat"
        exec(event['code'])
        response = wombat(event['state'], time_left)

        return {
            'response': response,
            'error': None
        }

    except Exception as e:
        exc_type, exc_value, exc_traceback = sys.exc_info()
        return {
            'response': None,
            'error': {
                'message': str(e),
                'stackTrace': traceback.format_exception(exc_type, exc_value, exc_traceback)
            }
        }

def lambda_handler(event, context):
    """ Entry point for Lambda function """
    time_left = context.get_remaining_time_in_millis
    return handle_event(event, time_left)
