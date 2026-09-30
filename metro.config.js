const path = require('path');
const {getDefaultConfig, mergeConfig} = require('@react-native/metro-config');

const adapters = {
  '@lodev09/react-native-true-sheet': path.resolve(__dirname, 'legacy/true-sheet.tsx'),
  '@shopify/flash-list': path.resolve(__dirname, 'legacy/flash-list.tsx'),
  'react-native-notify-kit': path.resolve(__dirname, 'legacy/notify-kit.ts'),
};

const originals = {
  'true-sheet-v2': '@lodev09/react-native-true-sheet',
  'flash-list-v1': '@shopify/flash-list',
};

const config = {
  resolver: {
    blockList: [
      /[\\/]node_modules[\\/].*[\\/](android|ios)[\\/]build[\\/].*/,
      /[\\/]android[\\/]build[\\/].*/,
      /[\\/]android[\\/]app[\\/]build[\\/].*/,
      /[\\/]ios[\\/]build[\\/].*/,
    ],
    resolveRequest: (context, moduleName, platform) => {
      if (adapters[moduleName]) {
        return {type: 'sourceFile', filePath: adapters[moduleName]};
      }
      if (originals[moduleName]) {
        return context.resolveRequest(context, originals[moduleName], platform);
      }
      return context.resolveRequest(context, moduleName, platform);
    },
  },
};

module.exports = mergeConfig(getDefaultConfig(__dirname), config);
