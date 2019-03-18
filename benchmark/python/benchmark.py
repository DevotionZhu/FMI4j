import datetime
import shutil
from fmpy import read_model_description, extract
from fmpy.fmi2 import FMU2Slave



class TestOptions:
    def __init__(self, fmu_filename, step_size, stop_time, vr):
        self.fmu_filename = fmu_filename
        self.step_size = step_size
        self.stop_time = stop_time
        self.vr = vr

options = [

    TestOptions(
        "../../fmus/2.0/cs/20sim/4.6.4.8004/ControlledTemperature/ControlledTemperature.fmu",1E-4, 10, 47),
    TestOptions(
        "../../fmus/2.0/cs/20sim/4.6.4.8004/TorsionBar/TorsionBar.fmu", 1E-5, 12, 2)

]


def main():

    option = options[1]

    model_description = read_model_description(option.fmu_filename)

    unzipdir = extract(option.fmu_filename)

    for i in range(0, 4):

        fmu = FMU2Slave(guid=model_description.guid,
                        unzipDirectory=unzipdir,
                        modelIdentifier=model_description.coSimulation.modelIdentifier,
                        instanceName='instance' + str(i))

        # initialize
        fmu.instantiate()
        fmu.setupExperiment(tolerance=1E-4, startTime=0.0, stopTime=option.stop_time)
        fmu.enterInitializationMode()
        fmu.exitInitializationMode()

        start = datetime.datetime.now()

        i = 0
        t = 0.0
        sum = 0.0
        # simulation loop
        while t <= (option.stop_time-option.step_size):

            i += 1
            # perform one step
            fmu.doStep(currentCommunicationPoint=t, communicationStepSize=option.step_size)

            sum += fmu.getReal([option.vr])[0]

            # advance the time
            t += option.step_size

        end = datetime.datetime.now()
        print("sum={}, iter={}".format(sum, i))

        delta = end - start
        print("{}ms".format(int(delta.total_seconds() * 1000)))

        try:
            fmu.terminate()
        except OSError:
            pass

    fmu.freeInstance()

    # clean up
    shutil.rmtree(unzipdir)


if __name__ == '__main__':
    main()