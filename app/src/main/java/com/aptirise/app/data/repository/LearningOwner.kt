package com.aptirise.app.data.repository

/** Local records belong to this installation; no account or network identity is needed. */
interface LearningOwner { val ownerId: String }
object DeviceLearningOwner : LearningOwner { override val ownerId = "guest" }
