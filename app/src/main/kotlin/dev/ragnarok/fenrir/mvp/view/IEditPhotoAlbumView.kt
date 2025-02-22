package dev.ragnarok.fenrir.mvp.view

import dev.ragnarok.fenrir.mvp.view.base.IAccountDependencyView
import dev.ragnarok.fenrir.mvp.view.base.ISteppersView
import dev.ragnarok.fenrir.view.steppers.impl.CreatePhotoAlbumStepsHost

interface IEditPhotoAlbumView : IAccountDependencyView, ISteppersView<CreatePhotoAlbumStepsHost>,
    IErrorView