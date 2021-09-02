package com.example.travelplanner

import android.os.Parcel
import android.os.Parcelable
import com.google.android.material.datepicker.CalendarConstraints
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class DateUtil {

    companion object {

        fun longToDate(date: Long): String {
            val date = Date(date)
            val format = SimpleDateFormat("MM/dd/yyyy")
            format.timeZone = TimeZone.getTimeZone("UTC")
            return format.format(date)
        }

        fun longToDateShortened(date: Long): String {
            val date = Date(date)
            val format = SimpleDateFormat("EEE MM/dd")
            format.timeZone = TimeZone.getTimeZone("UTC")
            return format.format(date)
        }

        fun dateToLong(date: String): Long {
            if (date.isEmpty()) {
                return 0
            }
            val format = SimpleDateFormat("MM/dd/yyyy")
            format.timeZone = TimeZone.getTimeZone("UTC")
            return format.parse(date).time
        }
    }
}

// https://stackoverflow.com/questions/36004398/restrict-range-in-android-datepicker-custom-dialog
class RangeValidator(private val minDate: Long, private val maxDate: Long) : CalendarConstraints.DateValidator {

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readLong()
    )

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        TODO("not implemented")
    }

    override fun describeContents(): Int {
        TODO("not implemented")
    }

    override fun isValid(date: Long): Boolean {
        return !(minDate > date || maxDate < date)
    }

    companion object CREATOR : Parcelable.Creator<RangeValidator> {
        override fun createFromParcel(parcel: Parcel): RangeValidator {
            return RangeValidator(parcel)
        }

        override fun newArray(size: Int): Array<RangeValidator?> {
            return arrayOfNulls(size)
        }
    }
}
