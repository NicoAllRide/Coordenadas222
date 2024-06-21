fun offlineValidationCard(
    cardId: String,
    community: ARCommunity?,
    route: PBRoute?
){
    val error: String
    var departure = departureProvider.getActiveDeparture()
    if(departure == null){
        error = context.getString(R.string.no_departures_found)
        listener?.onValidationError(
            context.getString(R.string.failed_validation),
            error
        )
        return
    }
    if(route == null){
        error = context.getString(R.string.no_route_found)
        listener?.onValidationError(
            context.getString(R.string.failed_validation),
            error
        )
        return
    }
    if(community == null){
        error = context.getString(R.string.no_community_found)
        listener?.onValidationError(
            context.getString(R.string.failed_validation),
            error
        )
        return
    }

    val departureId = if(departure.id.isNotEmpty()){
        departure.id
    }
    else if(!departure.offlineSync?.internalId.isNullOrEmpty()){
        departure.offlineSync?.internalId ?: ""
    }
    else{
        ""
    }
    val lastKnownLocation = TripService.instance?.lastKnownLocation ?: AllRideBusesApplication.instance.lastKnownLocation?.location
    val latitude    = lastKnownLocation?.latitude
    val longitude   = lastKnownLocation?.longitude
    val validation          = PBValidation().apply {
        this.id           = UUID.randomUUID().toString()
        this.communityId  = community.id
        this.routeId      = departure?.routeId ?: ""
        this.departureId  = departureId
        this.createdAt    = Date()
        this.validated    = false
        this.loc          = RealmFloatPair()
        this.departureTk  = departure?.accessToken ?: ""
        this.cardId       = cardId
        this.synced       = false
        this.reason = RealmList()
        if(lastKnownLocation != null) {
            this.latitude     = latitude?.toFloat()
            this.longitude    = longitude?.toFloat()
            this.loc          = RealmFloatPair().apply {
                this.float1 = longitude?.toFloat()
                this.float2 = latitude?.toFloat()
            }
        }
    }

    val user            = userProvider.getUserForCardId(cardId)
    if(user == null){
        validation.reason?.add("no_user_for_card")
        validationProvider.update(validation)
        error = context.getString(R.string.user_not_found)
        listener?.onValidationError(
            context.getString(R.string.failed_validation),
            error
        )
        return
    }
    val userCommunity = user.communities?.find{ it.card != null && (it?.card?.cardId == cardId.lowercase() || it.card?.cardId == cardId.uppercase()) }
    val newCommunity        = communityProvider.getCommunity(userCommunity?.communityId ?: "")
    if(newCommunity == null){
        error = context.getString(R.string.no_community_found)
        listener?.onValidationError(
            context.getString(R.string.failed_validation),
            error
        )
        return
    }
    val validationConfig = newCommunity.custom?.realTimeTransportSystem?.buses?.validation

    if(validationConfig?.enabled != true){
        error = context.getString(R.string.validation_not_allowed_in_this_community)
        listener?.onValidationError(
            context.getString(R.string.failed_validation),
            error
        )
        return
    }

    val usesTickets = validationConfig.usesTickets == true && route.usesTickets
    if(!usesTickets){
        error = context.getString(R.string.validation_with_tickets_not_allowed_in_this_community)
        listener?.onValidationError(
            context.getString(R.string.failed_validation),
            error
        )
        return
    }
    validation.userId = user.id
    val previousValidations = userProvider.getPreviousValidations(user.id)
    if(previousValidations.isNotEmpty()){
        validation.reason?.add("offline_validation_limit")
        updateValidation(validation)
        error = context.getString(R.string.offline_limit_reached)
        listener?.onValidationError(
            context.getString(R.string.failed_validation),
            error
        )
        return
    }

    if(validationConfig.departureLimit != null && validationConfig.departureLimit?.enabled == true){
        val previousDepartureValidation = departureProvider.getPreviousValidations(departure.id)
        if(previousDepartureValidation.size >= validationConfig.departureLimit!!.amount){
            validation.reason?.add("departure_limit")
            val userSkipCheck = validationConfig.departureLimit?.userSkip?.find{ it == user.id }
            if(validationConfig.departureLimit?.allowsSkip != true && userSkipCheck == null){
                validationProvider.update(validation)
                error = context.getString(R.string.validation_limit_per_departure_reached)
                listener?.onValidationError(
                    context.getString(R.string.failed_validation),
                    error
                )
                return
            }else if(userSkipCheck != null){
                validation.reason?.add("user_skip")
            }
        }
    }
    if((route.usesVehicleList || route.usesVehicleQRLink) && route.dynamicSeatAssignment && departure.vehicleId.isNotEmpty()){
        val checkIfAssigned = departure.enabledSeats.find{ it.userId == user.id }
        if(checkIfAssigned != null){
            validation.assignedSeat = checkIfAssigned.seat
            validationProvider.update(validation)
            departure = departureProvider.setAvailableSeat(checkIfAssigned,user.id)
        }else{
            val checkAvailableSeat = departure.enabledSeats.find{ it.available }
            if(checkAvailableSeat != null){
                validation.assignedSeat = checkAvailableSeat.seat
                validationProvider.update(validation)
                departure = departureProvider.setAvailableSeat(checkAvailableSeat,user.id)
            }else{
                error = context.getString(R.string.no_seats_available)
                listener?.onValidationError(
                    context.getString(R.string.failed_validation),
                    error
                )
                return
            }
        }
    }
    
    validation.validated = true
    validationProvider.update(validation)
    listener?.onValidationSuccess(validation,departure)
    return
}