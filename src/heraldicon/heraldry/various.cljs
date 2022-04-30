(ns heraldicon.heraldry.various
  (:require
   [heraldicon.interface :as interface]))

(defmethod interface/options-subscriptions :heraldry.component/helm [_context]
  #{})

(defmethod interface/options :heraldry.component/helm [_context]
  {})

(defmethod interface/options-subscriptions :heraldry.component/helms [_context]
  #{})

(defmethod interface/options :heraldry.component/helms [_context]
  {})

(defmethod interface/options-subscriptions :heraldry.component/ornaments [_context]
  #{})

(defmethod interface/options :heraldry.component/ornaments [_context]
  {})
