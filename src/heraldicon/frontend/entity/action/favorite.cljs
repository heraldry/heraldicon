(ns heraldicon.frontend.entity.action.favorite
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.user.session :as session]
   [heraldicon.static :as static]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def favorite-count-path
  [::store :counts])

(def user-favorites-path
  [::store :user-favorites])

(defn set-favorite-count
  [db entity-id favorite-count]
  (assoc-in db (conj favorite-count-path entity-id) favorite-count))

(defn set-is-user-favorite
  [db entity-id is-user-favorite]
  (assoc-in db (conj user-favorites-path entity-id) is-user-favorite))

(rf/reg-sub ::favorite-count
  (fn [[_ entity-id]]
    (rf/subscribe [:get (conj favorite-count-path entity-id)]))

  (fn [favorite-count _]
    (or favorite-count 0)))

(rf/reg-sub ::is-user-favorite?
  (fn [[_ entity-id]]
    (rf/subscribe [:get (conj user-favorites-path entity-id)]))

  (fn [is-user-favorite? _]
    is-user-favorite?))

(rf/reg-event-db ::update-favorite
  (fn [db [_ entity-id {:keys [is-user-favorite
                               favorite-count]}]]
    (-> db
        (set-favorite-count entity-id favorite-count)
        (set-is-user-favorite entity-id is-user-favorite))))

(rf/reg-fx ::toggle-favorite
  (fn [[entity-id session]]
    (go
      (try
        (let [;; TODO: this can probably live elsewhere
              payload {:entity-id entity-id}
              result (<? (api/call :toggle-favorite payload session))]
          (rf/dispatch [::update-favorite entity-id result]))

        (catch :default e
          (log/error e "favorite error"))))))

(rf/reg-event-fx ::invoke
  (fn [_ [_ entity-id session]]
    {::toggle-favorite [entity-id session]}))

(defn action [entity-id]
  (let [logged-in? @(rf/subscribe [::session/logged-in?])
        can-favorite? (and logged-in?
                           entity-id)
        session @(rf/subscribe [::session/data])]
    {:title :string.button/share
     :handler (when can-favorite?
                #(rf/dispatch [::invoke entity-id session]))
     :disabled? (not can-favorite?)
     :tooltip (when-not logged-in?
                :string.user.message/need-to-be-logged-in)}))

(defn icon [height on?]
  [:img {:src (static/static-url (if on?
                                   "/svg/favorite-on.svg"
                                   "/svg/favorite-off.svg"))
         :style {:height (str height "px")
                 :vertical-align "top"}}])

(defn button [entity-id & {:keys [height]
                           :or {height 24}}]
  (let [{:keys [handler
                disabled?
                tooltip]} (action entity-id)
        favorite-count @(rf/subscribe [::favorite-count entity-id])
        is-favorite? @(rf/subscribe [::is-user-favorite? entity-id])]
    [:button.button.favorite
     {:style {:flex "initial"
              :padding "5px"
              :line-height (str height "px")}
      :class (when disabled? "disabled")
      :title (tr tooltip)
      :on-click handler}
     [icon height is-favorite?]
     [:div {:style {:display "inline-block"
                    :padding-left "5px"
                    :font-size (str (* height 0.8) "px")
                    :vertical-align "top"}}
      favorite-count]]))
