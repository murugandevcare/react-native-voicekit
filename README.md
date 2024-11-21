# 🎤 react-native-voicekit

Recognize and transcribe user speech using React Native.

![npm bundle size](https://img.shields.io/bundlephobia/min/react-native-voicekit?style=flat-square) ![GitHub](https://img.shields.io/github/license/kuatsu/react-native-voicekit?style=flat-square) ![GitHub last commit](https://img.shields.io/github/last-commit/kuatsu/react-native-voicekit?style=flat-square)

> [!WARNING]
> This project is still considered unstable and under active development. The API might change drastically in new versions. Please proceed with caution.

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

Afterwards, add the config plugin to the `plugins` section of your `app.json` and `expo prebuild` or rebuild your development client.

## Quick Start

```tsx
import React from 'react';
import { View, Text, Button } from 'react-native';
import { useVoice, VoiceMode } from 'react-native-voicekit';

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

## Documentation

A documentation is work in progress.

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
