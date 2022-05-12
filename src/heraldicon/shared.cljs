(ns heraldicon.shared
  (:require
   ["paper" :refer [paper Size]]
   [heraldicon.entity.arms] ;; needed for side effects
   [heraldicon.entity.charge] ;; needed for side effects
   [heraldicon.entity.collection] ;; needed for side effects
   [heraldicon.entity.ribbon] ;; needed for side effects
   [heraldicon.heraldry.charge-group.core] ;; needed for side effects
   [heraldicon.heraldry.charge-group.options] ;; needed for side effects
   [heraldicon.heraldry.charge.core] ;; needed for side effects
   [heraldicon.heraldry.charge.options] ;; needed for side effects
   [heraldicon.heraldry.charge.other] ;; needed for side effects
   [heraldicon.heraldry.coat-of-arms] ;; needed for side effects
   [heraldicon.heraldry.field.core] ;; needed for side effects
   [heraldicon.heraldry.field.shared] ;; needed for side effects
   [heraldicon.heraldry.motto] ;; needed for side effects
   [heraldicon.heraldry.option.attributes] ;; needed for side effects
   [heraldicon.heraldry.ordinary.core] ;; needed for side effects
   [heraldicon.heraldry.ordinary.options] ;; needed for side effects
   [heraldicon.heraldry.ribbon] ;; needed for side effects
   [heraldicon.heraldry.semy] ;; needed for side effects
   [heraldicon.heraldry.tincture] ;; needed for side effects
   [heraldicon.heraldry.various] ;; needed for side effects
   [heraldicon.render.options] ;; needed for side effects
   [heraldicon.state] ;; needed for side effects
   ))

(.setup paper (new Size 500 500))
