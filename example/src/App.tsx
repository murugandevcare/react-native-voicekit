import { StyleSheet, View, Text, TouchableOpacity, TextInput } from 'react-native';
import { VoiceError, VoiceKit, VoiceMode, useVoice } from 'react-native-voicekit';
import Dropdown from './components/Dropdown';
import { useEffect, useState } from 'react';

export default function App() {
  const [locale, setLocale] = useState('en-US');
  const [supportedLocales, setSupportedLocales] = useState<string[]>([]);

  const { available, listening, transcript, startListening, stopListening, resetTranscript } = useVoice({
    locale,
    enablePartialResults: true,
    mode: VoiceMode.Continuous,
    silenceTimeoutMs: 1000,
  });

  useEffect(() => {
    VoiceKit.getSupportedLocales().then((locales) => setSupportedLocales(locales.sort()));
  }, []);

  return (
    <View style={styles.container}>
      <Text>Is available: {available ? 'Yes' : 'No'}</Text>
      <Text style={{ marginBottom: 30 }}>Is listening: {listening ? 'Yes' : 'No'}</Text>
      <Dropdown
        label="Locale"
        data={supportedLocales.map((l) => ({ label: l, value: l }))}
        maxHeight={300}
        value={locale}
        onChange={(item) => setLocale(item.value)}
        containerStyle={styles.dropdown}
        style={styles.dropdown}
      />
      <TouchableOpacity
        onPress={async () => {
          await startListening().catch((error) => {
            console.error('Error starting listening', error, error instanceof VoiceError ? error.details : null);
          });
        }}
        disabled={!available || listening}
        style={[styles.button, (!available || listening) && styles.disabledButton]}>
        <Text style={styles.buttonText}>Start Listening</Text>
      </TouchableOpacity>
      <TouchableOpacity
        onPress={() => stopListening()}
        disabled={!listening}
        style={[styles.button, !listening && styles.disabledButton]}>
        <Text style={styles.buttonText}>Stop Listening</Text>
      </TouchableOpacity>
      <TouchableOpacity onPress={() => resetTranscript()} style={styles.button}>
        <Text style={styles.buttonText}>Reset Transcript</Text>
      </TouchableOpacity>
      <TextInput multiline value={transcript} editable={false} style={styles.resultTextarea} placeholder="Transcript" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 10,
    paddingHorizontal: 20,
    backgroundColor: '#f0f0f0',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
  dropdown: {
    width: '100%',
  },
  button: {
    width: '100%',
    padding: 10,
    backgroundColor: 'lightblue',
    borderRadius: 5,
  },
  disabledButton: {
    backgroundColor: '#ccc',
  },
  buttonText: {
    textAlign: 'center',
  },
  resultTextarea: {
    width: '100%',
    height: 100,
    borderWidth: 1,
    borderColor: '#aaa',
    borderRadius: 10,
    padding: 10,
  },
});
