(ns heraldicon.frontend.user.session
  (:require
   [heraldicon.config :as config]
   [heraldicon.util.url :as url]
   [hodgepodge.core :refer [get-item local-storage remove-item set-item]]
   [re-frame.core :as rf]))

(def ^:private db-path
  [:user-data])

(def ^:private local-storage-session-id-name
  "cl-session-id")

(def ^:private local-storage-user-id-name
  "cl-user-id")

(def ^:private local-storage-username-name
  "cl-username")

(defn read []
  (let [session-id (get-item local-storage local-storage-session-id-name)
        user-id (get-item local-storage local-storage-user-id-name)
        username (get-item local-storage local-storage-username-name)
        relevant-url (or (config/get :heraldicon-site-url)
                         (config/get :heraldicon-url))]
    (when (some-> session-id count pos?)
      (set! js/document.cookie (str "session-id=" session-id
                                    ";domain=" (url/domain relevant-url)
                                    ";path=/")))
    (rf/dispatch-sync [:set db-path
                       (when (and session-id
                                  username
                                  user-id)
                         {:username username
                          :session-id session-id
                          :user-id user-id
                          :logged-in? true})])))

(defn store [session-data]
  (let [{:keys [session-id
                username
                user-id]} session-data]
    (set-item local-storage local-storage-session-id-name session-id)
    (set-item local-storage local-storage-username-name username)
    (set-item local-storage local-storage-user-id-name user-id))
  (read))

(defn clear []
  (remove-item local-storage local-storage-session-id-name)
  (remove-item local-storage local-storage-user-id-name)
  (remove-item local-storage local-storage-username-name)
  (rf/dispatch [:remove db-path]))
