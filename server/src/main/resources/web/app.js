import { ApiClient } from './api-client.js';
import { CallController } from './call-controller.js';
import { CallView } from './call-view.js';
import { Diagnostics } from './diagnostics.js';

const app = new CallController({
  api: new ApiClient(),
  view: new CallView(),
  diagnostics: new Diagnostics(),
});

app.start();
