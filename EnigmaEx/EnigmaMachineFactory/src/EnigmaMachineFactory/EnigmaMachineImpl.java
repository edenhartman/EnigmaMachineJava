package EnigmaMachineFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import EnigmaMachineFactory.Actual.*;

public class EnigmaMachineImpl implements EnigmaMachine {
    private Enigma enigma;
    private Secret secret;
    private List<Rotor> workingRotors;
    private Reflector workingReflector;
    private boolean debugModeOn = false;

    public EnigmaMachineImpl() {
        enigma = new Enigma();
    }
    public EnigmaMachineImpl(Enigma enigma) {
        this.enigma = enigma;
    }

    public Enigma getEnigma() {
        return enigma;
    }

    public Reflector getWorkingReflector() {
        return workingReflector;
    }

    public int getABCLength(){
        return enigma.getMachine().getAbc().length();
    }
    public int getNumOfRotors(){
        return enigma.getMachine().getRotorsSize();
    }
    public int getRotorsCount(){
        return enigma.getMachine().getRotorsCount();
    }
    @Override
    public SecretBuilder createSecret() {
        return new SecretBuilder(this);
    }

    @Override
    public Secret getSecret() {
        return secret;
    }

    @Override
    public void initFromSecret(Secret secret) {//TODO: implement

    }

    @Override
    public void resetToInitialPosition() {//TODO: implement

    }

    @Override
    public void setInitialPosition(String position) {//TODO: implement

    }

    @Override
    public String process(String plainText) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < plainText.length(); i++) {
            char c = plainText.charAt(i);
            sb.append(process(c));
        }
        return sb.toString();
    }
    /**
     * processes a signle character in the machine, using the current configured Secret
     * @param character The current char to process
     * @return the result of the char processing by the machine
     */
    @Override
    public char process(char character) {
        char result;
        int reflectorResult;
        moveFirstRotorAndCheckNotch();
        reflectorResult = startFromProcess(character);
        result = startToProcess(reflectorResult);
        return result;
    }

    private char startToProcess(int reflectorResult) {
        int position, finalLocation;
        int size = workingRotors.size();
        int abcLength =  getEnigma().getMachine().getAbc().length();
        int entry = reflectorResult;
        for (int i = 0; i < size; i++){
            Rotor rotor = workingRotors.get(size - 1 - i);
            position = rotor.getPosition();
            if (entry > position) {
                finalLocation = entry - position;
            } else {
                finalLocation = (abcLength - (position - entry))%abcLength;
            }
            char c = rotor.getMappingCharToPart(finalLocation);
            entry = rotor.getMapFromPositionByChar(c);
        }
        return getEnigma().getMachine().getAbc().charAt(entry);
    }

    private int startFromProcess(char character) {
        int position, finalLocation;
        int size = workingRotors.size();
        String abc =  getEnigma().getMachine().getAbc();
        int entry = abc.indexOf(character);
        for (int i = 0; i < size; i++){
            Rotor rotor = workingRotors.get(i);
            position = rotor.getPosition();
            if (entry > position) {
                finalLocation = entry - position;
            } else {
                finalLocation = (abc.length() - (position - entry))%abc.length();
            }
            char c = rotor.getMappingCharFromPart(finalLocation);
            entry = rotor.getMapToPositionByChar(c);
        }
        return workingReflector.getReflectToByPosition(entry);
    }

    private void moveFirstRotorAndCheckNotch() {
        boolean updateFinsied = false;
        Rotor rotor = workingRotors.get(0);
        int i = 1;
        rotor.increasePositionBy(1);
        if (rotor.isNotchAtPosition()) {
            while (!updateFinsied) {
                Rotor innerRotor = workingRotors.get(i);
                innerRotor.increasePositionBy(1);
                i++;
                if (!innerRotor.isNotchAtPosition()) {
                    updateFinsied = true;
                }
            }
        }
    }

    @Override
    public void setDebug(boolean debug) { //TODO: implement
        debugModeOn = debug;
    }


    @Override
    public void consumeState(Consumer<String> stateConsumer) {
        StringBuilder sb = new StringBuilder();
        sb.append(getWorkingRotors());
        sb.append(getWorkingRotorsChar());
        sb.append('<').append(workingReflector.getStringID()).append('>');
        stateConsumer.accept(sb.toString());
    }

    private String getWorkingRotorsChar() {
        StringBuilder sb = new StringBuilder();
        sb.append('<');
        List<Character> chars = new ArrayList<>();
        for (Rotor rotor : workingRotors){
            chars.add(rotor.getMappingCharFromPart(rotor.getPosition()));
        }
        List<Character> shallowCopy = chars.subList(0,chars.size());
        Collections.reverse(shallowCopy);
        for (Character character : shallowCopy){
            sb.append(character).append(',');
        }
        sb.deleteCharAt(sb.length()-1).append('>');
        return sb.toString();
    }

    private String getWorkingRotors() {
        StringBuilder sb = new StringBuilder();
        sb.append('<');
        List<Integer> selectedRotors = secret.getSelectedRotorsInOrder();
        List<Integer> shallowCopy = selectedRotors.subList(0, selectedRotors.size());
        Collections.reverse(shallowCopy);
        for (Integer integer : shallowCopy){
            sb.append(integer).append(',');
        }
        sb.deleteCharAt(sb.length()-1).append('>');
        return sb.toString();
    }

    public void setSecret(SecretImpl secret) {
        this.secret = secret;
        initSecretInMachine();
    }

    private void initSecretInMachine() {
        workingReflector = getEnigma().getMachine().getReflectorById(secret.getSelectedReflector());
        workingRotors = getEnigma().getMachine().getRotorsById(secret.getSelectedRotorsInOrder());
        setWorkingRotorsPostions();
        setWorkingRotorsFixedNotch();
    }

    private void setWorkingRotorsPostions() {
        int i = 0;
        for (Rotor rotor : workingRotors){
            rotor.increasePositionBy(secret.getSelectedRotorsPositions().get(i));
            i++;
        }
    }

    private void setWorkingRotorsFixedNotch() {
        for (Rotor rotor : workingRotors){
            int notchPosition = rotor.getNotch();
            int rotorPosition = rotor.getPosition();
            int result = (notchPosition > rotorPosition) ?
                    (notchPosition - rotorPosition) : (notchPosition + rotorPosition);
            rotor.setWorkingNotch(result);
        }
    }
}