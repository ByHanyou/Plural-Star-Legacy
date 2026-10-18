import {jest} from '@jest/globals';
import mockClipboard from '@react-native-clipboard/clipboard/jest/clipboard-mock.js';

jest.mock('@react-native-clipboard/clipboard', () => mockClipboard);

jest.mock('@react-native-async-storage/async-storage', () =>
  require('@react-native-async-storage/async-storage/jest'),
);

jest.mock('react-native-keyboard-controller', () =>
  require('react-native-keyboard-controller/jest'),
);

jest.mock('react-native-localize', () =>
  require('react-native-localize/mock/jest'),
);

jest.mock('@notifee/react-native', () =>
  require('@notifee/react-native/jest-mock'),
);

jest.mock('@react-native-community/image-editor', () => ({
  __esModule: true,
  default: {cropImage: jest.fn()},
}));

jest.mock('@react-native-community/geolocation', () => ({
  __esModule: true,
  default: {getCurrentPosition: jest.fn()},
}));

jest.mock('react-native-share', () => ({
  __esModule: true,
  default: {open: jest.fn(() => Promise.resolve())},
}));

jest.mock('react-native-blob-util', () => ({
  __esModule: true,
  default: {
    config: jest.fn(),
    fetch: jest.fn(),
    MediaCollection: {copyToMediaStore: jest.fn()},
    fs: {
      dirs: {DocumentDir: '/mock/documents', CacheDir: '/mock/cache'},
      exists: jest.fn(() => Promise.resolve(false)),
      mkdir: jest.fn(() => Promise.resolve()),
      ls: jest.fn(() => Promise.resolve([])),
      lstat: jest.fn(() => Promise.resolve([])),
      stat: jest.fn(() => Promise.resolve({size: 0})),
      readFile: jest.fn(() => Promise.resolve('')),
      writeFile: jest.fn(() => Promise.resolve()),
      appendFile: jest.fn(() => Promise.resolve()),
      readStream: jest.fn(() => Promise.resolve()),
      slice: jest.fn(() => Promise.resolve()),
      unlink: jest.fn(() => Promise.resolve()),
      cp: jest.fn(() => Promise.resolve()),
      mv: jest.fn(() => Promise.resolve()),
      hash: jest.fn(() => Promise.resolve('')),
    },
  },
}));
