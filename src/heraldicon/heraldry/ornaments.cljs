(ns heraldicon.heraldry.ornaments
  (:require
   [heraldicon.interface :as interface]))

(defmethod interface/options-subscriptions :heraldry.component/ornaments [_context]
  #{})

(defmethod interface/options :heraldry.component/ornaments [_context]
  {})
