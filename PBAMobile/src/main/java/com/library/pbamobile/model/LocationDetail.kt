package com.library.pbamobile.model

data class LocationDetail(
    val latitude:Double,
    val longitude:Double,
    val countryName:String,
    val locality:String,
    val subLocality:String,
    val postalCode:String,
    val adminArea:String,
)
