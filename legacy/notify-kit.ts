import type {TimestampTrigger as NotifeeTimestampTrigger} from '@notifee/react-native';

export * from '@notifee/react-native';
export {default} from '@notifee/react-native';

export type TimestampTrigger = NotifeeTimestampTrigger & {repeatInterval?: number};
