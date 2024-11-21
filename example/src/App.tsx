import { StyleSheet, View, Text, Button } from 'react-native';
import { VoiceError, VoiceMode, useVoice } from 'react-native-voicekit';

export default function App() {
  const { available, listening, transcript, startListening, stopListening, resetTranscript } = useVoice({
    locale: 'en-US',
    enablePartialResults: true,
    mode: VoiceMode.Continuous,
  });

  return (
    <View style={styles.container}>
      <Text>Is available: {available ? 'Yes' : 'No'}</Text>
      <Text>Is listening: {listening ? 'Yes' : 'No'}</Text>
      <Button
        title="Start Listening"
        onPress={async () => {
          await startListening().catch((error) => {
            console.error('Error starting listening', error, error instanceof VoiceError ? error.details : null);
          });
        }}
        disabled={!available || listening}
      />
      <Button title="Stop Listening" onPress={() => stopListening()} disabled={!listening} />
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
