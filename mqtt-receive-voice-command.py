import glob
import logging
import time
import requests

import paho.mqtt.client as mqtt

# log to a file
logging.basicConfig(level=logging.DEBUG,
                    format='%(asctime)s %(levelname)s %(message)s',
                    filename='/tmp/mqtt-receive-voice-command.log',
                    filemode='w')

# # log to std output
# logging.basicConfig(level=logging.DEBUG,
#                     format='%(asctime)s %(levelname)s %(message)s')

base_dir = '/sys/bus/w1/devices/'
device_folder = glob.glob(base_dir + '28*')[0]
device_file = device_folder + '/w1_slave'

topic_prefix = "xx@qq.com"
command_topic = topic_prefix + "/command"
status_topic = topic_prefix + "/status"

URL = "https://free.currencyconverterapi.com/api/v5/convert"


def read_temperature_raw():
    f = open(device_file, 'r')
    lines = f.readlines()
    f.close()
    return lines


def read_temperature():
    lines = read_temperature_raw()
    while lines[0].strip()[-3:] != 'YES':
        time.sleep(0.2)
        lines = read_temperature_raw()
    equals_pos = lines[1].find('t=')
    if equals_pos != -1:
        temp_string = lines[1][equals_pos + 2:]
        temp_c = float(temp_string) / 1000.0
        return temp_c


# This is the Subscriber

def on_connect(client, userdata, flags, rc):
    logging.debug("Connected with result code " + str(rc))
    client.subscribe(command_topic)
    logging.debug("subscribed to " + command_topic)


def on_message(client, userdata, msg):
    text = msg.payload.decode().lower()
    logging.debug("received text: " + text)
    logging.debug("recognized command: ")
    if "temperature" in text and "room" in text:
        logging.debug("temperature")
        temp = read_temperature()
        logging.debug(temp)
        client.publish(status_topic, temp)
        # client.disconnect()
    elif "euro" in text and ("cny" in text or "renminbi" in text):
        logging.debug("convert EUR to CNY")
        # sending get request and saving the response as response object
        params = {"compact": "y", "q": "EUR_CNY"}
        response = requests.get(url=URL, params=params)
        # extracting data in json format
        data = response.json()
        logging.debug(data)
        result = data['EUR_CNY']['val']
        logging.debug(result)
        client.publish(status_topic, result)
    else:
        logging.debug("unknown command")


client = mqtt.Client()



client.username_pw_set("xxx", "yyy")
client.connect("zzz.net", 1883, 60)

client.on_connect = on_connect
client.on_message = on_message

client.loop_forever()
