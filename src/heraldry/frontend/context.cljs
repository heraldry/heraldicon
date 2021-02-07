(ns heraldry.frontend.context
  (:require [heraldry.frontend.charge :as charge]
            [re-frame.core :as rf]))

(def default
  {:load-charge-data       charge/fetch-charge-data
   :fn-component-selected? #(do
                              @(rf/subscribe [:ui-component-selected? %]))
   :fn-select-component    #(rf/dispatch [:ui-component-select %])})
