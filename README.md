# 🎙️ react-native-voicekit

VoiceKit is a powerful speech recognition library for React Native that enables voice transcription across platforms. It provides direct access to native speech recognition APIs for optimal performance and aligns the API behavior between the different platforms.

![npm bundle size](https://img.shields.io/bundlephobia/min/react-native-voicekit?style=flat-square) ![GitHub](https://img.shields.io/github/license/kuatsu/react-native-voicekit?style=flat-square) ![GitHub last commit](https://img.shields.io/github/last-commit/kuatsu/react-native-voicekit?style=flat-square)


- ⚡ Real-time speech-to-text transcription
- 📱 iOS and Android support using native speech recognition APIs
- 🧪 Fully compatible with Expo
- ⚡ iOS and Android APIs aligned for consistent behavior and superior developer experience
- 🎛️ Single and continuous recognition modes
- 🎨 Simple and intuitive React Hooks API
- 💪 TypeScript support out of the box
- 👌 Lightweight with zero dependencies

## Installation

### React Native

```sh
npm install react-native-voicekit
cd ios && pod install
```

### Expo

```sh
npx expo install react-native-voicekit
```

Afterwards, add the config plugin to the `plugins` section of your `app.json`:

```json
{
  "plugins": [
    [
      "react-native-voicekit",
      {
        "speechRecognitionPermission": "Custom iOS speech recognition permission message (optional)",
        "microphonePermission": "Custom iOS microphone permission message (optional)"
      }
    ]
  ]
}
```

Finally, `expo prebuild` or rebuild your development client.

## Quick Start

```tsx
import React from 'react';
import { View, Text, Button } from 'react-native';
import { useVoice, VoiceMode } from 'aitl-react-native-voicekit';

const App = () => {
  const { available, listening, transcript, startListening, stopListening } = useVoice({
    locale: 'en-US',
    mode: VoiceMode.Continuous,
    enablePartialResults: true,
  });

  return (
    <View>
      {available ? (
        <>
          <Text>Is listening: {listening ? 'Yes' : 'No'}</Text>
          <Text>Transcript: {transcript}</Text>
          <Button onPress={startListening} title="Start Listening" />
          <Button onPress={stopListening} title="Stop Listening" />
        </>
      ) : (
        <Text>Speech recognition is not available on this device.</Text>
      )}
    </View>
  );
};
```

## License

MIT
