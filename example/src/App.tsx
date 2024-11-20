import { StyleSheet, View, Text, Button } from 'react-native';
import { VoiceError, useVoice } from 'react-native-voicekit';

export default function App() {
  const { available, transcript, startListening, stopListening, resetTranscript } = useVoice({
    enablePartialResults: false,
  });

  return (
    <View style={styles.container}>
      <Text>Is available: {available ? 'Yes' : 'No'}</Text>
      <Button
        title="Start Listening"
        onPress={async () => {
          await startListening({ locale: 'de-DE' }).catch((error) => {
            console.error('Error starting listening', error, error instanceof VoiceError ? error.details : null);
          });
        }}
      />
      <Button title="Stop Listening" onPress={() => stopListening()} />
      <Button title="Reset Transcript" onPress={() => resetTranscript()} />
      <Text>Transcript: {transcript}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
