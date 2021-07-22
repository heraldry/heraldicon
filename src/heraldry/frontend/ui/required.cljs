(ns heraldry.frontend.ui.required
  (:require
   [heraldry.attribution] ;; needed for defmethods
   [heraldry.coat-of-arms.charge-group.core] ;; needed for defmethods
   [heraldry.coat-of-arms.charge-group.options] ;; needed for defmethods
   [heraldry.coat-of-arms.charge.core] ;; needed for defmethods
   [heraldry.coat-of-arms.charge.options] ;; needed for defmethods
   [heraldry.coat-of-arms.core] ;; needed for defmethods
   [heraldry.coat-of-arms.field.options] ;; needed for defmethods
   [heraldry.coat-of-arms.field.shared] ;; needed for defmethods
   [heraldry.coat-of-arms.ordinary.core] ;; needed for defmethods
   [heraldry.coat-of-arms.ordinary.options] ;; needed for defmethods
   [heraldry.coat-of-arms.semy.core] ;; needed for defmethods
   [heraldry.coat-of-arms.semy.options] ;; needed for defmethods
   [heraldry.frontend.state :as state] ;; needed for events
   [heraldry.frontend.ui.element.attributes] ;; needed for defmethods
   [heraldry.frontend.ui.form.arms-general] ;; needed for defmethods
   [heraldry.frontend.ui.form.charge-general] ;; needed for defmethods
   [heraldry.frontend.ui.form.collection-general] ;; needed for defmethods
   [heraldry.render-options] ;; needed for defmethods
   ))
