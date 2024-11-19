import { useEffect, useState } from 'react';
import { StyleSheet, View, Text, Button } from 'react-native';
import { VoiceKit, VoiceKitError, VoiceKitEvent } from 'react-native-voicekit';

export default function App() {
  const [result, setResult] = useState<string | undefined>();

  useEffect(() => {
    VoiceKit.addListener(VoiceKitEvent.PartialResult, (newResult) => {
      console.log('Partial result', newResult);
    });

    VoiceKit.addListener(VoiceKitEvent.Result, (newResult) => {
      console.log('Final result', newResult);
      setResult(newResult);
    });
  }, []);

  return (
    <View style={styles.container}>
      <Button
        title="Start Listening"
        onPress={async () => {
          console.log('Starting listening');
          await VoiceKit.startListening({ locale: 'de-DE' }).catch((error) => {
            console.error(
              'Error starting listening',
              error,
              error instanceof VoiceKitError ? error.details : null
            );
          });
          console.log('Started listening');
        }}
      />
      <Button title="Stop Listening" onPress={() => VoiceKit.stopListening()} />
      <Text>Result: {result}</Text>
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
