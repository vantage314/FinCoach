import type { App } from 'vue'
import FcCard from './FcCard.vue'
import FcTable from './FcTable.vue'
import FcInput from './FcInput.vue'
import FcSelect from './FcSelect.vue'
import FcDatePicker from './FcDatePicker.vue'

export { FcCard, FcTable, FcInput, FcSelect, FcDatePicker }

export default {
    install(app: App) {
        app.component('FcCard', FcCard)
        app.component('FcTable', FcTable)
        app.component('FcInput', FcInput)
        app.component('FcSelect', FcSelect)
        app.component('FcDatePicker', FcDatePicker)
    }
}
