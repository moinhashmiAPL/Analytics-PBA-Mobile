package com.library.pbamobile.model


//Manufacturer of device
//2. Brand
//3. Model
//4. Hardware
//5. Serial No.
//6. Android Id
//7. Screen Resolution
//8. Screen Density
//9. User
//10. Host
//11. API Level
//12. Build ID
//13. Build Time.

data class DeviceInfo(
        val manufacturer:String,
        val brand: String,
        val model: String,
        val hardware: String,
        val serialNo: String,
        val androidID: String,
        val screenResolution: String,
        val screenDensity: Int,
        val user: String,
        val host: String,
        val apiLevel: Int,
        val buildID: String,
        val buildTime: Long,
        val securityPatch: String,
        val bootLoader: String,
        val ram: String,
        val operatorList:List<Operator>,
        val numberOfSim:Int,
        )
