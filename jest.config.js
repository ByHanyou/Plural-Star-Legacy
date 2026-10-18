module.exports = {
  preset: 'react-native',
  setupFiles: [
    '<rootDir>/jest.setup.js',
    './node_modules/@react-native-documents/picker/jest/build/jest/setup.js',
  ],
  transformIgnorePatterns: [
    'node_modules/(?!((jest-)?react-native|@react-native(-community)?|@react-native-async-storage|react-native-keyboard-controller|@notifee|@noble/hashes|react-native-get-random-values|react-native-image-picker|react-native-linear-gradient|@react-native-documents)/)',
  ],
  moduleNameMapper: {
    '^@lodev09/react-native-true-sheet$': '<rootDir>/legacy/true-sheet.tsx',
    '^@shopify/flash-list$': '<rootDir>/legacy/flash-list.tsx',
    '^react-native-notify-kit$': '<rootDir>/legacy/notify-kit.ts',
    '^true-sheet-v2$': '<rootDir>/node_modules/@lodev09/react-native-true-sheet',
    '^flash-list-v1$': '<rootDir>/node_modules/@shopify/flash-list',
  },
};
