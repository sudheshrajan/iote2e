Initial targets

High Level Goals
+ Generate test data via java - one reading per second, configurable patient id sets
+ speed and batch layers just for open mhealth
+ speed layer - 
	rules above a certain level, 
	stream a single user/single measure (or maybe just fixed to BP)
+ batch layer
	write all values to db -single table or multiple?
+ real time UI
	initially for a single user/single measure
	add selectable patient
+ tableau or qlikview
	subjects, measures on composite graph over time
	
Open Questions



Next Steps
+ WS to accept inbound binary and send to Kafka
+ modify RunSim to connect and send to WS
+ Batch layer - write to separate tables based on OMH Schema
+ Speed Layer
	1. for a specific user and BP, write the systolic and diastolic to WS #2
	2. Create WS page to get the systolic and diastolic, plot in real time
	3. When exceeded, send email to Dr. Zybrick
	