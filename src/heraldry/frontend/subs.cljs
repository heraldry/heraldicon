(ns heraldry.frontend.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :get
 (fn [db [_ path]]
   (get-in db path)))
