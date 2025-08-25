package com.example.runningtrackerapp.ui.fragments

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.runningtrackerapp.Adapter.RunAdapter
import com.example.runningtrackerapp.R
import com.example.runningtrackerapp.others.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.example.runningtrackerapp.others.SortTypes
import com.example.runningtrackerapp.others.TrackingUtility
import com.example.runningtrackerapp.ui.viewModels.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions


@AndroidEntryPoint
class RunFragment: Fragment(R.layout.fragment_run){
    private val viewModel: MainViewModel by viewModels()
    private lateinit var rvRuns: RecyclerView
    private lateinit var runAdapter: RunAdapter
    private lateinit var spFilter: Spinner

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvRuns = view.findViewById(R.id.rvRuns)
        val fab = view.findViewById<FloatingActionButton>(R.id.fab)
        spFilter = view.findViewById(R.id.spFilter)
        fab.setOnClickListener {
            findNavController().navigate(R.id.action_runFragment_to_trackingFragment)
        }
        setupRecyclerView()
        when(viewModel.sortType){
            SortTypes.DATE -> spFilter.setSelection(0)
            SortTypes.RUNNING_TIME -> spFilter.setSelection(1)
            SortTypes.DISTANCE -> spFilter.setSelection(2)
            SortTypes.AVG_SPEED -> spFilter.setSelection(3)
            SortTypes.CALORIES_BURNED -> spFilter.setSelection(4)
        }
        spFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                when(pos) {
                    0 -> viewModel.sortRuns(SortTypes.DATE)
                    1 -> viewModel.sortRuns(SortTypes.RUNNING_TIME)
                    2 -> viewModel.sortRuns(SortTypes.DISTANCE)
                    3 -> viewModel.sortRuns(SortTypes.AVG_SPEED)
                    4 -> viewModel.sortRuns(SortTypes.CALORIES_BURNED)
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
        viewModel.runs.observe(viewLifecycleOwner, Observer {
              runAdapter.submitList(it)
              rvRuns.smoothScrollToPosition(0)
        })
    }

    private fun setupRecyclerView() = rvRuns.apply{
        runAdapter = RunAdapter()
        adapter = runAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }


}