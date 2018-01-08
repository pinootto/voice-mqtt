# voice-mqtt
A voice interface for MQTT

This project creates an Android app which does the following:
 * it recognizes the speech and sends the text to a MQTT broker
 * it receives the text from a MQTT broker and converts it to speech

The python program implements the command listener:
 * it receives the text from the MQTT broker and recognizes some commands
 * it executes the commands and sends the result to the MQTT broker
