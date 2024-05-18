package com.topstep.wearkit.sample.model

class ContactsBean internal constructor(
    val name: String,
    val number: String,
    var isdelete: Boolean
) {
    companion object {
        internal const val NAME_BYTES_LIMIT = 32
        internal const val NUMBER_BYTES_LIMIT = 20


        /**
         * Create a contact that the device can recognize. If the incoming parameter is invalid, it will return null.
         *
         * @param name   Contact name
         * @param number Contact phone number
         * @return
         */
        fun create(name: String?, number: String?): ContactsBean? {
            if (name == null || number == null) return null
            val resultName = subString(name.trim(), NAME_BYTES_LIMIT)
            val resultNumber = subString(number.trim(), NUMBER_BYTES_LIMIT)
            return if (resultName.isNullOrEmpty() || resultNumber.isNullOrEmpty()) {
                null
            } else {
                ContactsBean(name, number, false)
            }
        }

        private fun subString(s: String?, bytesNeed: Int): String? {
            if (s == null || s.isEmpty() || bytesNeed <= 0) return null
            val bytes = s.toByteArray()
            if (bytes.size < bytesNeed) return s
            var count = 0
            for (i in s.indices) {
                count += s[i].toString().toByteArray().size
                if (count == bytesNeed) {
                    return s.substring(0, i + 1)
                } else if (count > bytesNeed) {
                    return if (i == 0) {
                        null
                    } else {
                        s.substring(0, i)
                    }
                }
            }
            return s
        }
    }
}