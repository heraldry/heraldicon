(ns heraldry.metadata
  (:require [heraldry.gettext :refer [string]]))

(def known-metadata-keys
  ["source"
   "source url"
   "blazonry"
   "armiger"
   "house"
   "location name"
   "location latitude"
   "location longitude"
   "year exact"
   "year approximate"
   "year min"
   "year max"])

(defn options [_context]
  {:ui {:label (string "Metadata")
        :form-type :metadata}})
