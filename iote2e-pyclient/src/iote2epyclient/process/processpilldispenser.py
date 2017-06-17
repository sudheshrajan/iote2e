import logging
import time
import base64
import uuid
from io import BytesIO
from picamera import PiCamera
from iote2epyclient.launch.clientutils import ClientUtils
from iote2epyclient.schema.iote2erequest import Iote2eRequest
from iote2epyclient.pilldispenser.blinkledthread import BlinkLedThread
from iote2epyclient.pilldispenser.buttonpushedthread import ButtonPushedThread
from iote2epyclient.pilldispenser.handlepilldispenser import HandlePillDispenser


logger = logging.getLogger(__name__)

class ProcessPillDispenser(object):
    '''
    classdocs
    '''

    def __init__(self, loginVo, sensorName):
        self.loginVo = loginVo
        self.sensorName = sensorName
        self.dispenseState = None
        self.numPillsToDispense = -1
        self.pillsDispensedUuid = None
        self.pillsDispensedDelta = 9999
        self.blinkLedThread = None
        self.buttonPushedThread = None
        self.handlePillDispenser = HandlePillDispenser()
        self.camera = PiCamera(resolution=(200,200))
        self.camera.contrast = 0
        self.camera.sharpness = 100

        
    def createIote2eRequest(self ):
        logger.info('ProcessPillDispenser dispenseState: ' + str(self.dispenseState) )
        iote2eRequest = None
        if None == self.dispenseState:
            time.sleep(1)
        elif 'DISPENSING' == self.dispenseState:
            # Tell the pill dispenser to dispense the number of pills
            self.handlePillDispenser.dispensePills(self.numPillsToDispense)
            # Sleep for half a second, then take a picture
            time.sleep(.5)
            # Byte64 encode the picture
            imageByte64 = self.takePicture()
            #with open("/home/pete/development/gitrepo/iote2e/iote2e-tests/iote2e-shared/images/iote2e-test4.png", "rb") as image_file:
            #    imageByte64 = base64.b64encode(image_file.read())
            # Create Iote2eRequest that contains the confirmation image
            self.dispenseState = 'DISPENSED_PENDING'
            pairs = { self.sensorName: imageByte64}
            metadata = { 'PILLS_DISPENSED_UUID': self.pillsDispensedUuid, 'PILLS_DISPENSED_STATE' : 'DISPENSED', 'NUM_PILLS_TO_DISPENSE' : str(self.numPillsToDispense) }
            iote2eRequest = Iote2eRequest( login_name=self.loginVo.loginName,source_name=self.loginVo.sourceName, source_type='pill_dispenser', 
                               request_uuid=str(uuid.uuid4()), 
                               request_timestamp=ClientUtils.nowIso8601(), 
                               pairs=pairs, metadata=metadata, operation='SENSORS_VALUES')
        elif 'DISPENSED' == self.dispenseState:
            logger.info('self.pillsDispensedDelta: ' + str(self.pillsDispensedDelta) )
            if self.pillsDispensedDelta == 0:
                msg = 'Correct number of pills dispensed'
                logger.info( msg )
                self.blinkLedThread = BlinkLedThread(ledColor='green')
                self.blinkLedThread.start()
            else:
                if self.pillsDispensedDelta < 0:
                    msg = "Not enough pills dispensed"
                else:
                    msg = "Too many pills dispensed"
                logger.info( msg )
                self.blinkLedThread = BlinkLedThread(ledColor='red')
                self.blinkLedThread.start()
            # Wait for button being pushed on separate thread
            self.dispenseState = 'CONFIRMED_PENDING'
            self.buttonPushedThread = ButtonPushedThread( self )
            self.buttonPushedThread.start()

        elif 'CONFIRMING' == self.dispenseState:
            pairs = { self.sensorName: '' }
            metadata = { 'PILLS_DISPENSED_UUID': self.pillsDispensedUuid, 'PILLS_DISPENSED_STATE' : 'CONFIRMED'}
            iote2eRequest = Iote2eRequest( login_name=self.loginVo.loginName,source_name=self.loginVo.sourceName, source_type='pill_dispenser', 
                               request_uuid=str(uuid.uuid4()), 
                               request_timestamp=ClientUtils.nowIso8601(), 
                               pairs=pairs, metadata=metadata, operation='ACTUATOR_CONFIRM')
            self.dispenseState = 'CONFIRMED_PENDING'
            time.sleep(.25)
        elif 'CONFIRMED' == self.dispenseState:
            self.dispenseState = None
            time.sleep(.25)
        else:
            time.sleep(1)
        return iote2eRequest


    def handleIote2eResult(self, iote2eResult ):
        logger.info('ProcessPillDispenser handleIote2eResult: ' + str(iote2eResult))
        pills_dispensed_state = iote2eResult.metadata['PILLS_DISPENSED_STATE']
        if 'DISPENSING' == pills_dispensed_state:
            self.numPillsToDispense = int(iote2eResult.pairs['actuatorValue'])
            self.pillsDispensedUuid = iote2eResult.metadata['PILLS_DISPENSED_UUID']
            self.dispenseState = 'DISPENSING'
        elif 'DISPENSED' == pills_dispensed_state:
            self.pillsDispensedDelta = int(iote2eResult.pairs['actuatorValue'])
            self.dispenseState = 'DISPENSED'
        elif 'CONFIRMED' == pills_dispensed_state:
            if self.blinkLedThread != None:
                self.blinkLedThread.shutdown()
                self.blinkLedThread = None
            self.pillsDispensedDelta = 9999
            self.dispenseState = 'CONFIRMED'


    def takePicture(self):
        logger.info('ProcessPillDispenser taking picture of dispensed pills')
        imageStream = BytesIO()
        self.camera.capture( imageStream, 'png' )
        imageByte64 = base64.b64encode(imageStream.getvalue() )
        imageStream.close()
        return imageByte64;
        

    def setDispenseState( self, dispenseState ):
        self.dispenseState = dispenseState
        